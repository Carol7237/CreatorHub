package com.creatorhub.contentservice.service.impl;

import com.creatorhub.common.Viewer;
import com.creatorhub.common.exception.BusinessRuleException;
import com.creatorhub.contentservice.client.SubscriptionAccessService;
import com.creatorhub.contentservice.dto.PostRequest;
import com.creatorhub.contentservice.dto.PostResponse;
import com.creatorhub.contentservice.model.Post;
import com.creatorhub.contentservice.repository.PostRepository;
import com.creatorhub.contentservice.repository.TagRepository;
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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PostServiceImpl}, including premium gating. The
 * inter-service access check ({@link SubscriptionAccessService}) is mocked, so the
 * gating LOGIC is verified in isolation (no network, no circuit breaker).
 */
@ExtendWith(MockitoExtension.class)
class PostServiceImplTest {

    @Mock private PostRepository postRepository;
    @Mock private TagRepository tagRepository;
    @Mock private SubscriptionAccessService subscriptionAccessService;

    @InjectMocks private PostServiceImpl postService;

    private static final Long AUTHOR = 1L;
    private static final Long TIER = 10L;

    private static Post premiumPost() {
        Post p = new Post();
        p.setId(5L);
        p.setTitle("Secret");
        p.setBody("hidden body");
        p.setPremium(true);
        p.setAuthorId(AUTHOR);
        p.setTierId(TIER);
        return p;
    }

    @Test
    void create_premiumWithoutTier_throws() {
        PostRequest req = PostRequest.builder().title("t").premium(true).tierId(null).build();
        assertThrows(BusinessRuleException.class, () -> postService.create(req, new Viewer(AUTHOR, false)));
        verify(postRepository, never()).save(any());
    }

    @Test
    void create_freeWithTier_throws() {
        PostRequest req = PostRequest.builder().title("t").premium(false).tierId(TIER).build();
        assertThrows(BusinessRuleException.class, () -> postService.create(req, new Viewer(AUTHOR, false)));
        verify(postRepository, never()).save(any());
    }

    @Test
    void create_setsAuthorFromViewer() {
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> {
            Post p = inv.getArgument(0);
            p.setId(7L);
            return p;
        });
        PostResponse resp = postService.create(
                PostRequest.builder().title("Free").premium(false).build(), new Viewer(99L, false));
        assertThat(resp.getCreatorId()).isEqualTo(99L);
        assertThat(resp.isPremium()).isFalse();
    }

    @Test
    void gating_admin_seesBody() {
        when(postRepository.findById(5L)).thenReturn(Optional.of(premiumPost()));
        PostResponse resp = postService.findById(5L, new Viewer(999L, true));
        assertThat(resp.isLocked()).isFalse();
        assertThat(resp.getBody()).isEqualTo("hidden body");
    }

    @Test
    void gating_author_seesBody() {
        when(postRepository.findById(5L)).thenReturn(Optional.of(premiumPost()));
        PostResponse resp = postService.findById(5L, new Viewer(AUTHOR, false));
        assertThat(resp.isLocked()).isFalse();
        assertThat(resp.getBody()).isEqualTo("hidden body");
    }

    @Test
    void gating_activeSubscriber_seesBody() {
        when(postRepository.findById(5L)).thenReturn(Optional.of(premiumPost()));
        when(subscriptionAccessService.hasActiveSubscription(2L, TIER)).thenReturn(true);
        PostResponse resp = postService.findById(5L, new Viewer(2L, false));
        assertThat(resp.isLocked()).isFalse();
        assertThat(resp.getBody()).isEqualTo("hidden body");
    }

    @Test
    void gating_nonSubscriber_locked() {
        when(postRepository.findById(5L)).thenReturn(Optional.of(premiumPost()));
        when(subscriptionAccessService.hasActiveSubscription(2L, TIER)).thenReturn(false);
        PostResponse resp = postService.findById(5L, new Viewer(2L, false));
        assertThat(resp.isLocked()).isTrue();
        assertThat(resp.getBody()).isNull();
    }

    @Test
    void gating_anonymous_locked_noInterServiceCall() {
        when(postRepository.findById(5L)).thenReturn(Optional.of(premiumPost()));
        PostResponse resp = postService.findById(5L, Viewer.anonymous());
        assertThat(resp.isLocked()).isTrue();
        assertThat(resp.getBody()).isNull();
        verify(subscriptionAccessService, never()).hasActiveSubscription(any(), any());
    }

    @Test
    void update_byNonOwner_throwsAccessDenied() {
        when(postRepository.findById(5L)).thenReturn(Optional.of(premiumPost()));
        lenient().when(subscriptionAccessService.hasActiveSubscription(any(), any())).thenReturn(false);
        PostRequest req = PostRequest.builder().title("hack").premium(true).tierId(TIER).build();
        assertThrows(AccessDeniedException.class, () -> postService.update(5L, req, new Viewer(2L, false)));
    }
}
