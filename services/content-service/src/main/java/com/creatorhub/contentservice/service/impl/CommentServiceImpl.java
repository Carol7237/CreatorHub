package com.creatorhub.contentservice.service.impl;

import com.creatorhub.common.Viewer;
import com.creatorhub.common.exception.ResourceNotFoundException;
import com.creatorhub.contentservice.client.SubscriptionAccessService;
import com.creatorhub.contentservice.dto.CommentRequest;
import com.creatorhub.contentservice.dto.CommentResponse;
import com.creatorhub.contentservice.dto.mapper.CommentMapper;
import com.creatorhub.contentservice.model.Comment;
import com.creatorhub.contentservice.model.Post;
import com.creatorhub.contentservice.repository.CommentRepository;
import com.creatorhub.contentservice.repository.PostRepository;
import com.creatorhub.contentservice.service.CommentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final SubscriptionAccessService subscriptionAccessService;

    @Override
    public CommentResponse create(CommentRequest request, Viewer viewer) {
        Post post = postRepository.findById(request.getPostId())
                .orElseThrow(() -> new ResourceNotFoundException("Post", request.getPostId()));

        // Premium posts: only the author, an admin, or an active subscriber may comment.
        if (post.isPremium() && !canComment(post, viewer)) {
            log.warn("Rejected comment on premium post {} by user {}", post.getId(), viewer.userId());
            throw new AccessDeniedException("Only active subscribers can comment on this premium post");
        }

        Comment comment = new Comment();
        comment.setText(request.getText());
        comment.setPost(post);
        comment.setAuthorId(viewer.userId());

        Comment saved = commentRepository.save(comment);
        log.info("Comment created: id={} post={} author={}", saved.getId(), post.getId(), saved.getAuthorId());
        return CommentMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentResponse> findByPost(Long postId) {
        return commentRepository.findByPostId(postId).stream().map(CommentMapper::toResponse).toList();
    }

    @Override
    public CommentResponse update(Long id, CommentRequest request, Viewer viewer) {
        Comment comment = getOrThrow(id);
        assertOwnerOrAdmin(comment, viewer);
        if (request.getText() != null) {
            comment.setText(request.getText());
        }
        return CommentMapper.toResponse(comment);
    }

    @Override
    public void delete(Long id, Viewer viewer) {
        Comment comment = getOrThrow(id);
        assertOwnerOrAdmin(comment, viewer);
        commentRepository.delete(comment);
    }

    /** Same access rule as premium body gating, via the inter-service call. */
    private boolean canComment(Post post, Viewer viewer) {
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

    private void assertOwnerOrAdmin(Comment comment, Viewer viewer) {
        if (!viewer.isOwnerOrAdmin(comment.getAuthorId())) {
            throw new AccessDeniedException("You can only modify your own comments");
        }
    }

    private Comment getOrThrow(Long id) {
        return commentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", id));
    }
}
