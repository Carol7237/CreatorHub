package com.creatorhub.service.impl;

import com.creatorhub.common.Viewer;
import com.creatorhub.dto.CommentRequest;
import com.creatorhub.dto.CommentResponse;
import com.creatorhub.exception.ResourceNotFoundException;
import com.creatorhub.model.Comment;
import com.creatorhub.model.Post;
import com.creatorhub.model.SubscriptionTier;
import com.creatorhub.model.User;
import com.creatorhub.model.enums.SubStatus;
import com.creatorhub.repository.CommentRepository;
import com.creatorhub.repository.PostRepository;
import com.creatorhub.repository.SubscriptionRepository;
import com.creatorhub.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentServiceImplTest {

    @Mock private CommentRepository commentRepository;
    @Mock private PostRepository postRepository;
    @Mock private UserRepository userRepository;
    @Mock private SubscriptionRepository subscriptionRepository;

    @InjectMocks private CommentServiceImpl commentService;

    private static User user(Long id) {
        User u = new User();
        u.setId(id);
        u.setUsername("u" + id);
        return u;
    }

    private static Post freePost(Long id) {
        Post p = new Post();
        p.setId(id);
        p.setPremium(false);
        p.setAuthor(user(1L));
        return p;
    }

    private static Post premiumPost(Long id) {
        Post p = new Post();
        p.setId(id);
        p.setPremium(true);
        p.setAuthor(user(1L));
        SubscriptionTier t = new SubscriptionTier();
        t.setId(5L);
        t.setCreator(user(1L));
        p.setTier(t);
        return p;
    }

    private static Viewer viewer(Long id) {
        return new Viewer(id, false);
    }

    private static CommentRequest req(Long postId) {
        return CommentRequest.builder().text("nice").postId(postId).build();
    }

    @Test
    void create_onFreePost_succeeds() {
        when(postRepository.findById(1L)).thenReturn(Optional.of(freePost(1L)));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L)));
        when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> {
            Comment c = inv.getArgument(0);
            c.setId(9L);
            return c;
        });
        CommentResponse response = commentService.create(req(1L), viewer(2L));
        assertThat(response.getId()).isEqualTo(9L);
        assertThat(response.getAuthorId()).isEqualTo(2L);
    }

    @Test
    void create_onPremiumPost_byNonSubscriber_throwsAccessDenied() {
        when(postRepository.findById(1L)).thenReturn(Optional.of(premiumPost(1L)));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L)));
        when(subscriptionRepository.existsByFanIdAndTierIdAndStatus(2L, 5L, SubStatus.ACTIVE)).thenReturn(false);
        assertThrows(AccessDeniedException.class, () -> commentService.create(req(1L), viewer(2L)));
        verify(commentRepository, never()).save(any());
    }

    @Test
    void create_onPremiumPost_bySubscriber_succeeds() {
        when(postRepository.findById(1L)).thenReturn(Optional.of(premiumPost(1L)));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L)));
        when(subscriptionRepository.existsByFanIdAndTierIdAndStatus(2L, 5L, SubStatus.ACTIVE)).thenReturn(true);
        when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> inv.getArgument(0));
        assertThat(commentService.create(req(1L), viewer(2L))).isNotNull();
    }

    @Test
    void create_onPremiumPost_byAuthor_succeeds() {
        when(postRepository.findById(1L)).thenReturn(Optional.of(premiumPost(1L)));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> inv.getArgument(0));
        assertThat(commentService.create(req(1L), viewer(1L))).isNotNull(); // viewer == post author
    }

    @Test
    void create_postNotFound_throws() {
        when(postRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> commentService.create(req(1L), viewer(2L)));
    }

    @Test
    void findById_notFound_throws() {
        when(commentRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> commentService.findById(99L));
    }

    @Test
    void update_byOwner_changesText() {
        Comment comment = new Comment();
        comment.setId(9L);
        comment.setText("old");
        comment.setPost(freePost(1L));
        comment.setAuthor(user(2L));
        when(commentRepository.findById(9L)).thenReturn(Optional.of(comment));
        CommentResponse response = commentService.update(9L,
                CommentRequest.builder().text("updated").postId(1L).build(), viewer(2L));
        assertThat(response.getText()).isEqualTo("updated");
    }

    @Test
    void update_byNonOwner_throwsAccessDenied() {
        Comment comment = new Comment();
        comment.setId(9L);
        comment.setAuthor(user(2L));
        when(commentRepository.findById(9L)).thenReturn(Optional.of(comment));
        assertThrows(AccessDeniedException.class, () -> commentService.update(9L,
                CommentRequest.builder().text("x").postId(1L).build(), viewer(999L)));
    }

    @Test
    void delete_byOwner_deletes() {
        Comment comment = new Comment();
        comment.setId(9L);
        comment.setAuthor(user(2L));
        when(commentRepository.findById(9L)).thenReturn(Optional.of(comment));
        commentService.delete(9L, viewer(2L));
        verify(commentRepository).delete(comment);
    }
}
