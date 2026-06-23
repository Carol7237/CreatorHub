package com.creatorhub.service.impl;

import com.creatorhub.common.Viewer;
import com.creatorhub.dto.CommentRequest;
import com.creatorhub.dto.CommentResponse;
import com.creatorhub.dto.mapper.CommentMapper;
import com.creatorhub.exception.ResourceNotFoundException;
import com.creatorhub.model.Comment;
import com.creatorhub.model.Post;
import com.creatorhub.model.User;
import com.creatorhub.model.enums.SubStatus;
import com.creatorhub.repository.CommentRepository;
import com.creatorhub.repository.PostRepository;
import com.creatorhub.repository.SubscriptionRepository;
import com.creatorhub.repository.UserRepository;
import com.creatorhub.service.CommentService;
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
    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;

    @Override
    public CommentResponse create(CommentRequest request, Viewer viewer) {
        Post post = postRepository.findById(request.getPostId())
                .orElseThrow(() -> new ResourceNotFoundException("Post", request.getPostId()));
        User author = userRepository.findById(viewer.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User", viewer.userId()));

        // Premium posts: only the author, an admin, or an active subscriber may comment.
        if (post.isPremium() && !canComment(post, viewer)) {
            log.warn("Rejected comment on premium post {} by user {}", post.getId(), viewer.userId());
            throw new AccessDeniedException("Only active subscribers can comment on this premium post");
        }

        Comment comment = new Comment();
        comment.setText(request.getText());
        comment.setPost(post);
        comment.setAuthor(author);

        Comment saved = commentRepository.save(comment);
        log.info("Comment created: id={} post={} author={}", saved.getId(), post.getId(), author.getId());
        return CommentMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public CommentResponse findById(Long id) {
        return CommentMapper.toResponse(getOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentResponse> findAll() {
        return commentRepository.findAll().stream().map(CommentMapper::toResponse).toList();
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

    private boolean canComment(Post post, Viewer viewer) {
        if (viewer.admin()) {
            return true;
        }
        Long uid = viewer.userId();
        if (post.getAuthor() != null && uid.equals(post.getAuthor().getId())) {
            return true;
        }
        if (post.getTier() == null) {
            return false;
        }
        return subscriptionRepository.existsByFanIdAndTierIdAndStatus(uid, post.getTier().getId(), SubStatus.ACTIVE);
    }

    private void assertOwnerOrAdmin(Comment comment, Viewer viewer) {
        Long ownerId = comment.getAuthor() != null ? comment.getAuthor().getId() : null;
        if (!viewer.isOwnerOrAdmin(ownerId)) {
            throw new AccessDeniedException("You can only modify your own comments");
        }
    }

    private Comment getOrThrow(Long id) {
        return commentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", id));
    }
}
