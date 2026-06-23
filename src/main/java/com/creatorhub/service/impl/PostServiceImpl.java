package com.creatorhub.service.impl;

import com.creatorhub.common.PageableUtils;
import com.creatorhub.common.Viewer;
import com.creatorhub.dto.PagedResponse;
import com.creatorhub.dto.PostRequest;
import com.creatorhub.dto.PostResponse;
import com.creatorhub.dto.mapper.PostMapper;
import com.creatorhub.exception.BusinessRuleException;
import com.creatorhub.exception.ResourceNotFoundException;
import com.creatorhub.model.Post;
import com.creatorhub.model.SubscriptionTier;
import com.creatorhub.model.Tag;
import com.creatorhub.model.User;
import com.creatorhub.model.enums.SubStatus;
import com.creatorhub.repository.PostRepository;
import com.creatorhub.repository.SubscriptionRepository;
import com.creatorhub.repository.SubscriptionTierRepository;
import com.creatorhub.repository.TagRepository;
import com.creatorhub.repository.UserRepository;
import com.creatorhub.service.PostService;
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

    /** Whitelisted sort properties for posts (never exposes internal fields). */
    private static final Set<String> ALLOWED_SORT = Set.of("id", "title", "createdAt", "premium");

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final SubscriptionTierRepository tierRepository;
    private final TagRepository tagRepository;
    private final SubscriptionRepository subscriptionRepository;

    @Override
    public PostResponse create(PostRequest request, Viewer viewer) {
        User author = userRepository.findById(viewer.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User", viewer.userId()));

        SubscriptionTier tier = resolveTierForPost(request.isPremium(), request.getTierId(), author);

        Post post = new Post();
        post.setTitle(request.getTitle());
        post.setBody(request.getBody());
        post.setPremium(request.isPremium());
        post.setAuthor(author);
        post.setTier(tier);
        post.setTags(resolveTags(request.getTags()));

        Post saved = postRepository.save(post);
        log.info("Post published: id={} creator={} premium={}", saved.getId(), author.getId(), saved.isPremium());
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
        log.debug("findAll posts page={} size={} sort={}", safe.getPageNumber(), safe.getPageSize(), safe.getSort());
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

        // Re-apply the premium/tier invariants against the (possibly new) values.
        SubscriptionTier tier = resolveTierForPost(request.isPremium(), request.getTierId(), post.getAuthor());
        post.setPremium(request.isPremium());
        post.setTier(tier);

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
     * Body of a premium post is visible only to: the author, an ADMIN, or a fan
     * with an ACTIVE subscription to the post's tier.
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
        if (post.getAuthor() != null && uid.equals(post.getAuthor().getId())) {
            return true;
        }
        if (post.getTier() == null) {
            return false;
        }
        return subscriptionRepository.existsByFanIdAndTierIdAndStatus(uid, post.getTier().getId(), SubStatus.ACTIVE);
    }

    private void assertOwnerOrAdmin(Post post, Viewer viewer) {
        Long ownerId = post.getAuthor() != null ? post.getAuthor().getId() : null;
        if (!viewer.isOwnerOrAdmin(ownerId)) {
            throw new AccessDeniedException("You can only modify your own posts");
        }
    }

    private SubscriptionTier resolveTierForPost(boolean premium, Long tierId, User author) {
        if (premium && tierId == null) {
            throw new BusinessRuleException("A premium post must reference a tier");
        }
        if (!premium && tierId != null) {
            throw new BusinessRuleException("A free post must not reference a tier");
        }
        if (tierId == null) {
            return null;
        }
        SubscriptionTier tier = tierRepository.findById(tierId)
                .orElseThrow(() -> new ResourceNotFoundException("SubscriptionTier", tierId));
        if (tier.getCreator() == null || !tier.getCreator().getId().equals(author.getId())) {
            throw new BusinessRuleException(
                    "Tier " + tierId + " does not belong to the post author " + author.getId());
        }
        return tier;
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
