package com.creatorhub.contentservice.service.impl;

import com.creatorhub.common.Viewer;
import com.creatorhub.common.exception.ResourceNotFoundException;
import com.creatorhub.contentservice.client.NotificationPublisher;
import com.creatorhub.contentservice.client.SubscriptionAccessService;
import com.creatorhub.contentservice.dto.CommentRequest;
import com.creatorhub.contentservice.dto.CommentResponse;
import com.creatorhub.contentservice.model.Comment;
import com.creatorhub.contentservice.model.Post;
import com.creatorhub.contentservice.repository.CommentRepository;
import com.creatorhub.contentservice.repository.PostRepository;
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
    @Mock private SubscriptionAccessService subscriptionAccessService;
    @Mock private NotificationPublisher notificationPublisher;

    @InjectMocks private CommentServiceImpl commentService;

    private static Post post(boolean premium, Long authorId, Long tierId) {
        Post p = new Post();
        p.setId(5L);
        p.setPremium(premium);
        p.setAuthorId(authorId);
        p.setTierId(tierId);
        return p;
    }

    private static CommentRequest req() {
        return CommentRequest.builder().text("nice").postId(5L).build();
    }

    @Test
    void create_onFreePost_ok() {
        when(postRepository.findById(5L)).thenReturn(Optional.of(post(false, 1L, null)));
        when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> {
            Comment c = inv.getArgument(0);
            c.setId(20L);
            return c;
        });
        CommentResponse resp = commentService.create(req(), new Viewer(2L, false));
        assertThat(resp.getAuthorId()).isEqualTo(2L);
        assertThat(resp.getPostId()).isEqualTo(5L);
    }

    @Test
    void create_onPremiumPost_byNonSubscriber_throws() {
        when(postRepository.findById(5L)).thenReturn(Optional.of(post(true, 1L, 10L)));
        when(subscriptionAccessService.hasActiveSubscription(2L, 10L)).thenReturn(false);
        assertThrows(AccessDeniedException.class, () -> commentService.create(req(), new Viewer(2L, false)));
        verify(commentRepository, never()).save(any());
    }

    @Test
    void create_onPremiumPost_bySubscriber_ok() {
        when(postRepository.findById(5L)).thenReturn(Optional.of(post(true, 1L, 10L)));
        when(subscriptionAccessService.hasActiveSubscription(2L, 10L)).thenReturn(true);
        when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> {
            Comment c = inv.getArgument(0);
            c.setId(21L);
            return c;
        });
        CommentResponse resp = commentService.create(req(), new Viewer(2L, false));
        assertThat(resp.getId()).isEqualTo(21L);
    }

    @Test
    void create_postNotFound_throws() {
        when(postRepository.findById(5L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> commentService.create(req(), new Viewer(2L, false)));
    }

    @Test
    void update_byNonOwner_throwsAccessDenied() {
        Comment c = new Comment();
        c.setId(20L);
        c.setAuthorId(1L);
        when(commentRepository.findById(20L)).thenReturn(Optional.of(c));
        assertThrows(AccessDeniedException.class, () -> commentService.update(20L,
                CommentRequest.builder().text("hack").postId(5L).build(), new Viewer(999L, false)));
    }
}
