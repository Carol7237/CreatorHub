package com.creatorhub.contentservice.service.impl;

import com.creatorhub.common.PageableUtils;
import com.creatorhub.common.Viewer;
import com.creatorhub.common.dto.PagedResponse;
import com.creatorhub.common.exception.BusinessRuleException;
import com.creatorhub.common.exception.ResourceNotFoundException;
import com.creatorhub.contentservice.client.SubscriptionAccessService;
import com.creatorhub.contentservice.dto.PostRequest;
import com.creatorhub.contentservice.dto.PostResponse;
import com.creatorhub.contentservice.dto.mapper.PostMapper;
import com.creatorhub.contentservice.model.Post;
import com.creatorhub.contentservice.model.Tag;
import com.creatorhub.contentservice.repository.PostRepository;
import com.creatorhub.contentservice.repository.TagRepository;
import com.creatorhub.contentservice.service.PostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class PostServiceImpl implements PostService {

    private static final Set<String> ALLOWED_SORT = Set.of("id", "title", "createdAt", "premium");

    private final PostRepository postRepository;
    private final TagRepository tagRepository;
    private final SubscriptionAccessService subscriptionAccessService;

    @Override
    public PostResponse create(PostRequest request, Viewer viewer) {
        validatePremiumInvariant(request.isPremium(), request.getTierId());

        Post post = new Post();
        post.setTitle(request.getTitle());
        post.setBody(request.getBody());
        post.setPremium(request.isPremium());
        // Owner from the authenticated context, never the request body.
        post.setAuthorId(viewer.userId());
        post.setTierId(request.isPremium() ? request.getTierId() : null);
        post.setTags(resolveTags(request.getTags()));

        Post saved = postRepository.save(post);
        log.info("Post published: id={} author={} premium={}", saved.getId(), saved.getAuthorId(), saved.isPremium());
        return toResponse(saved, viewer);
    }

    @Override
    @Transactional(readOnly = true)
    public PostResponse findById(Long id, Viewer viewer) {
        return toResponse(getOrThrow(id), viewer);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<PostResponse> findAll(Pageable pageable, Viewer viewer) {
        Pageable safe = PageableUtils.sanitize(pageable, ALLOWED_SORT);
        return PagedResponse.from(postRepository.findAll(safe).map(post -> toResponse(post, viewer)));
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<PostResponse> findByCreator(Long creatorId, Pageable pageable, Viewer viewer) {
        Pageable safe = PageableUtils.sanitize(pageable, ALLOWED_SORT);
        return PagedResponse.from(postRepository.findByAuthorId(creatorId, safe).map(post -> toResponse(post, viewer)));
    }

    @Override
    public PostResponse update(Long id, PostRequest request, Viewer viewer) {
        Post post = getOrThrow(id);
        assertOwnerOrAdmin(post, viewer);

        if (request.getTitle() != null) {
            post.setTitle(request.getTitle());
        }
        if (request.getBody() != null) {
            post.setBody(request.getBody());
        }
        validatePremiumInvariant(request.isPremium(), request.getTierId());
        post.setPremium(request.isPremium());
        post.setTierId(request.isPremium() ? request.getTierId() : null);
        if (request.getTags() != null) {
            post.setTags(resolveTags(request.getTags()));
        }
        return toResponse(post, viewer);
    }

    @Override
    public void delete(Long id, Viewer viewer) {
        Post post = getOrThrow(id);
        assertOwnerOrAdmin(post, viewer);
        postRepository.delete(post);
        log.info("Post deleted: id={} by user={}", id, viewer.userId());
    }

    // --- premium gating ---

    /** Maps a post, hiding the body of premium posts the viewer cannot access. */
    private PostResponse toResponse(Post post, Viewer viewer) {
        PostResponse dto = PostMapper.toResponse(post);
        if (post.isPremium() && !canAccessBody(post, viewer)) {
            dto.setBody(null);
            dto.setLocked(true);
        }
        return dto;
    }

    /**
     * Body of a premium post is visible only to: the author, an ADMIN, or a fan with
     * an ACTIVE subscription to the post's tier. The last case is an inter-service
     * call to the Subscription service (circuit-breaker protected, fail-closed).
     */
    private boolean canAccessBody(Post post, Viewer viewer) {
        if (!post.isPremium()) {
            return true;
        }
        if (viewer.admin()) {
            return true;
        }
        Long uid = viewer.userId();
        if (uid == null) {
            return false;
        }
        if (uid.equals(post.getAuthorId())) {
            return true;
        }
        if (post.getTierId() == null) {
            return false;
        }
        return subscriptionAccessService.hasActiveSubscription(uid, post.getTierId());
    }

    private void assertOwnerOrAdmin(Post post, Viewer viewer) {
        if (!viewer.isOwnerOrAdmin(post.getAuthorId())) {
            throw new AccessDeniedException("You can only modify your own posts");
        }
    }

    /**
     * Premium/free invariant. NOTE: validating that the tier actually belongs to the
     * author is now cross-service (the tier lives in the Subscription service) and is
     * deferred — a bogus tierId simply leaves the post locked for fans (fail-closed),
     * which is not a security hole.
     */
    private void validatePremiumInvariant(boolean premium, Long tierId) {
        if (premium && tierId == null) {
            throw new BusinessRuleException("A premium post must reference a tier");
        }
        if (!premium && tierId != null) {
            throw new BusinessRuleException("A free post must not reference a tier");
        }
    }

    /** Get-or-create tags by name (trimmed, blanks ignored). */
    private Set<Tag> resolveTags(Set<String> names) {
        if (names == null || names.isEmpty()) {
            return new HashSet<>();
        }
        Set<String> wanted = names.stream()
                .filter(n -> n != null && !n.isBlank())
                .map(String::trim)
                .collect(Collectors.toCollection(HashSet::new));
        if (wanted.isEmpty()) {
            return new HashSet<>();
        }

        Set<Tag> result = new HashSet<>(tagRepository.findByNameIn(wanted));
        Set<String> existingNames = result.stream().map(Tag::getName).collect(Collectors.toSet());
        for (String name : wanted) {
            if (!existingNames.contains(name)) {
                Tag tag = new Tag();
                tag.setName(name);
                result.add(tagRepository.save(tag));
            }
        }
        return result;
    }

    private Post getOrThrow(Long id) {
        return postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post", id));
    }
}
