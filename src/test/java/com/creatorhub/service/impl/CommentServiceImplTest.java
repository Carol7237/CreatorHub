package com.creatorhub.service.impl;

import com.creatorhub.dto.CommentRequest;
import com.creatorhub.dto.CommentResponse;
import com.creatorhub.exception.ResourceNotFoundException;
import com.creatorhub.model.Comment;
import com.creatorhub.model.Post;
import com.creatorhub.model.User;
import com.creatorhub.repository.CommentRepository;
import com.creatorhub.repository.PostRepository;
import com.creatorhub.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentServiceImplTest {

    @Mock private CommentRepository commentRepository;
    @Mock private PostRepository postRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private CommentServiceImpl commentService;

    private static Post post(Long id) {
        Post p = new Post();
        p.setId(id);
        return p;
    }

    private static User user(Long id) {
        User u = new User();
        u.setId(id);
        u.setUsername("u" + id);
        return u;
    }

    @Test
    void create_valid_succeeds() {
        when(postRepository.findById(1L)).thenReturn(Optional.of(post(1L)));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L)));
        when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> {
            Comment c = inv.getArgument(0);
            c.setId(9L);
            return c;
        });

        CommentResponse response = commentService.create(
                CommentRequest.builder().text("nice").postId(1L).authorId(2L).build());

        assertThat(response.getId()).isEqualTo(9L);
        assertThat(response.getText()).isEqualTo("nice");
        assertThat(response.getPostId()).isEqualTo(1L);
        assertThat(response.getAuthorId()).isEqualTo(2L);
    }

    @Test
    void create_postNotFound_throws() {
        when(postRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> commentService.create(
                CommentRequest.builder().text("x").postId(1L).authorId(2L).build()));
    }

    @Test
    void create_authorNotFound_throws() {
        when(postRepository.findById(1L)).thenReturn(Optional.of(post(1L)));
        when(userRepository.findById(2L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> commentService.create(
                CommentRequest.builder().text("x").postId(1L).authorId(2L).build()));
    }

    @Test
    void findById_notFound_throws() {
        when(commentRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> commentService.findById(99L));
    }

    @Test
    void update_changesText() {
        Comment comment = new Comment();
        comment.setId(9L);
        comment.setText("old");
        comment.setPost(post(1L));
        comment.setAuthor(user(2L));
        when(commentRepository.findById(9L)).thenReturn(Optional.of(comment));

        CommentResponse response = commentService.update(9L,
                CommentRequest.builder().text("updated").build());

        assertThat(response.getText()).isEqualTo("updated");
        assertThat(comment.getText()).isEqualTo("updated");
    }

    @Test
    void delete_deletes() {
        Comment comment = new Comment();
        comment.setId(9L);
        when(commentRepository.findById(9L)).thenReturn(Optional.of(comment));
        commentService.delete(9L);
        verify(commentRepository).delete(comment);
    }
}
