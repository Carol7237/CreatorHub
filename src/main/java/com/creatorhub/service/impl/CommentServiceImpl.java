package com.creatorhub.service.impl;

import com.creatorhub.dto.CommentRequest;
import com.creatorhub.dto.CommentResponse;
import com.creatorhub.dto.mapper.CommentMapper;
import com.creatorhub.exception.ResourceNotFoundException;
import com.creatorhub.model.Comment;
import com.creatorhub.model.Post;
import com.creatorhub.model.User;
import com.creatorhub.repository.CommentRepository;
import com.creatorhub.repository.PostRepository;
import com.creatorhub.repository.UserRepository;
import com.creatorhub.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * NOTE: enforcing that only active subscribers may comment on a premium post is
 * deliberately left out of this phase (it is an access-control concern handled
 * with Security/Views later). Comment creation only validates that the post and
 * author exist.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    @Override
    public CommentResponse create(CommentRequest request) {
        Post post = postRepository.findById(request.getPostId())
                .orElseThrow(() -> new ResourceNotFoundException("Post", request.getPostId()));
        User author = userRepository.findById(request.getAuthorId())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.getAuthorId()));

        Comment comment = new Comment();
        comment.setText(request.getText());
        comment.setPost(post);
        comment.setAuthor(author);

        return CommentMapper.toResponse(commentRepository.save(comment));
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
    public CommentResponse update(Long id, CommentRequest request) {
        Comment comment = getOrThrow(id);
        // Only the text is mutable; post and author are fixed for a given comment.
        if (request.getText() != null) {
            comment.setText(request.getText());
        }
        return CommentMapper.toResponse(comment);
    }

    @Override
    public void delete(Long id) {
        Comment comment = getOrThrow(id);
        commentRepository.delete(comment);
    }

    private Comment getOrThrow(Long id) {
        return commentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", id));
    }
}
