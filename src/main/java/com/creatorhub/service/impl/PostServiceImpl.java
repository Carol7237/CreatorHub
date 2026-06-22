package com.creatorhub.service.impl;

import com.creatorhub.dto.PostRequest;
import com.creatorhub.dto.PostResponse;
import com.creatorhub.dto.mapper.PostMapper;
import com.creatorhub.exception.BusinessRuleException;
import com.creatorhub.exception.ResourceNotFoundException;
import com.creatorhub.model.Post;
import com.creatorhub.model.SubscriptionTier;
import com.creatorhub.model.Tag;
import com.creatorhub.model.User;
import com.creatorhub.repository.PostRepository;
import com.creatorhub.repository.SubscriptionTierRepository;
import com.creatorhub.repository.TagRepository;
import com.creatorhub.repository.UserRepository;
import com.creatorhub.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final SubscriptionTierRepository tierRepository;
    private final TagRepository tagRepository;

    @Override
    public PostResponse create(PostRequest request) {
        User author = userRepository.findById(request.getAuthorId())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.getAuthorId()));

        SubscriptionTier tier = resolveTierForPost(request.isPremium(), request.getTierId(), author);

        Post post = new Post();
        post.setTitle(request.getTitle());
        post.setBody(request.getBody());
        post.setPremium(request.isPremium());
        post.setAuthor(author);
        post.setTier(tier);
        post.setTags(resolveTags(request.getTags()));

        return PostMapper.toResponse(postRepository.save(post));
    }

    @Override
    @Transactional(readOnly = true)
    public PostResponse findById(Long id) {
        return PostMapper.toResponse(getOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PostResponse> findAll() {
        return postRepository.findAll().stream().map(PostMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PostResponse> findByCreator(Long creatorId) {
        return postRepository.findByAuthorId(creatorId).stream().map(PostMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PostResponse> findByPremium(boolean premium) {
        return postRepository.findByPremium(premium).stream().map(PostMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PostResponse> findByCreatorAndPremium(Long creatorId, boolean premium) {
        return postRepository.findByAuthorIdAndPremium(creatorId, premium).stream()
                .map(PostMapper::toResponse).toList();
    }

    @Override
    public PostResponse update(Long id, PostRequest request) {
        Post post = getOrThrow(id);

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

        return PostMapper.toResponse(post);
    }

    @Override
    public void delete(Long id) {
        // Comments cascade (orphanRemoval); join rows in post_tags are removed by
        // the owning side, while the tags themselves stay (shared). So a plain
        // delete is safe here.
        Post post = getOrThrow(id);
        postRepository.delete(post);
    }

    /**
     * Enforces: a premium post must have a tier; a free post must not; and the
     * tier must belong to the post's author.
     */
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
