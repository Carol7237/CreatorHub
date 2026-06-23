package com.creatorhub.service.impl;

import com.creatorhub.common.Viewer;
import com.creatorhub.dto.PostRequest;
import com.creatorhub.dto.PostResponse;
import com.creatorhub.exception.BusinessRuleException;
import com.creatorhub.exception.ResourceNotFoundException;
import com.creatorhub.model.Post;
import com.creatorhub.model.SubscriptionTier;
import com.creatorhub.model.User;
import com.creatorhub.model.enums.Role;
import com.creatorhub.model.enums.SubStatus;
import com.creatorhub.repository.PostRepository;
import com.creatorhub.repository.SubscriptionRepository;
import com.creatorhub.repository.SubscriptionTierRepository;
import com.creatorhub.repository.TagRepository;
import com.creatorhub.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostServiceImplTest {

    @Mock private PostRepository postRepository;
    @Mock private UserRepository userRepository;
    @Mock private SubscriptionTierRepository tierRepository;
    @Mock private TagRepository tagRepository;
    @Mock private SubscriptionRepository subscriptionRepository;

    @InjectMocks private PostServiceImpl postService;

    private static User creator(Long id) {
        User u = new User();
        u.setId(id);
        u.setUsername("creator" + id);
        u.setRole(Role.USER);
        return u;
    }

    private static SubscriptionTier tier(Long id, User creator) {
        SubscriptionTier t = new SubscriptionTier();
        t.setId(id);
        t.setName("VIP");
        t.setPriceMonthly(new BigDecimal("9.99"));
        t.setCreator(creator);
        return t;
    }

    private static Post premiumPost(Long id, User author, SubscriptionTier tier) {
        Post p = new Post();
        p.setId(id);
        p.setTitle("Premium");
        p.setBody("secret body");
        p.setPremium(true);
        p.setAuthor(author);
        p.setTier(tier);
        return p;
    }

    private static Viewer viewer(Long id) {
        return new Viewer(id, false);
    }

    // --- create rules ---

    @Test
    void create_premiumWithoutTier_throws() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(creator(1L)));
        PostRequest req = PostRequest.builder().title("t").body("b").premium(true).build();
        assertThrows(BusinessRuleException.class, () -> postService.create(req, viewer(1L)));
        verify(postRepository, never()).save(any());
    }

    @Test
    void create_freeWithTier_throws() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(creator(1L)));
        PostRequest req = PostRequest.builder().title("t").body("b").premium(false).tierId(5L).build();
        assertThrows(BusinessRuleException.class, () -> postService.create(req, viewer(1L)));
    }

    @Test
    void create_premiumWithForeignTier_throws() {
        User author = creator(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(author));
        when(tierRepository.findById(5L)).thenReturn(Optional.of(tier(5L, creator(2L))));
        PostRequest req = PostRequest.builder().title("t").body("b").premium(true).tierId(5L).build();
        assertThrows(BusinessRuleException.class, () -> postService.create(req, viewer(1L)));
    }

    @Test
    void create_validFreePost_succeeds() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(creator(1L)));
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> {
            Post p = inv.getArgument(0);
            p.setId(100L);
            return p;
        });
        PostResponse response = postService.create(
                PostRequest.builder().title("Hello").body("b").premium(false).build(), viewer(1L));
        assertThat(response.isPremium()).isFalse();
        assertThat(response.isLocked()).isFalse();
        assertThat(response.getCreatorId()).isEqualTo(1L);
    }

    @Test
    void create_authorNotFound_throws() {
        when(userRepository.findById(42L)).thenReturn(Optional.empty());
        PostRequest req = PostRequest.builder().title("t").body("b").premium(false).build();
        assertThrows(ResourceNotFoundException.class, () -> postService.create(req, viewer(42L)));
    }

    // --- premium gating on read ---

    @Test
    void findById_premium_anonymous_isLocked() {
        when(postRepository.findById(10L)).thenReturn(Optional.of(premiumPost(10L, creator(1L), tier(5L, creator(1L)))));
        PostResponse r = postService.findById(10L, Viewer.anonymous());
        assertThat(r.isLocked()).isTrue();
        assertThat(r.getBody()).isNull();
        assertThat(r.getTitle()).isEqualTo("Premium"); // metadata still visible
    }

    @Test
    void findById_premium_author_seesBody() {
        when(postRepository.findById(10L)).thenReturn(Optional.of(premiumPost(10L, creator(1L), tier(5L, creator(1L)))));
        PostResponse r = postService.findById(10L, viewer(1L)); // viewer == author
        assertThat(r.isLocked()).isFalse();
        assertThat(r.getBody()).isEqualTo("secret body");
    }

    @Test
    void findById_premium_admin_seesBody() {
        when(postRepository.findById(10L)).thenReturn(Optional.of(premiumPost(10L, creator(1L), tier(5L, creator(1L)))));
        PostResponse r = postService.findById(10L, new Viewer(99L, true)); // admin
        assertThat(r.isLocked()).isFalse();
        assertThat(r.getBody()).isEqualTo("secret body");
    }

    @Test
    void findById_premium_activeSubscriber_seesBody() {
        when(postRepository.findById(10L)).thenReturn(Optional.of(premiumPost(10L, creator(1L), tier(5L, creator(1L)))));
        when(subscriptionRepository.existsByFanIdAndTierIdAndStatus(7L, 5L, SubStatus.ACTIVE)).thenReturn(true);
        PostResponse r = postService.findById(10L, viewer(7L));
        assertThat(r.isLocked()).isFalse();
        assertThat(r.getBody()).isEqualTo("secret body");
    }

    @Test
    void findById_premium_nonSubscriber_isLocked() {
        when(postRepository.findById(10L)).thenReturn(Optional.of(premiumPost(10L, creator(1L), tier(5L, creator(1L)))));
        when(subscriptionRepository.existsByFanIdAndTierIdAndStatus(7L, 5L, SubStatus.ACTIVE)).thenReturn(false);
        PostResponse r = postService.findById(10L, viewer(7L));
        assertThat(r.isLocked()).isTrue();
        assertThat(r.getBody()).isNull();
    }

    @Test
    void findById_freePost_alwaysVisible() {
        Post free = new Post();
        free.setId(11L);
        free.setTitle("Free");
        free.setBody("open");
        free.setPremium(false);
        free.setAuthor(creator(1L));
        when(postRepository.findById(11L)).thenReturn(Optional.of(free));
        PostResponse r = postService.findById(11L, Viewer.anonymous());
        assertThat(r.isLocked()).isFalse();
        assertThat(r.getBody()).isEqualTo("open");
    }

    @Test
    void findById_notFound_throws() {
        when(postRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> postService.findById(99L, Viewer.anonymous()));
    }

    @Test
    void findAll_paged_returnsPagedResponse() {
        Pageable pageable = PageRequest.of(0, 10);
        Post free = new Post();
        free.setId(1L);
        free.setPremium(false);
        free.setAuthor(creator(1L));
        when(postRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(free), pageable, 1));
        var page = postService.findAll(pageable, Viewer.anonymous());
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getTotalElements()).isEqualTo(1);
    }

    @Test
    void findByCreator_paged_delegates() {
        Pageable pageable = PageRequest.of(0, 10);
        when(postRepository.findByAuthorId(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));
        assertThat(postService.findByCreator(1L, pageable, Viewer.anonymous()).getContent()).isEmpty();
    }

    // --- ownership on update/delete ---

    @Test
    void update_byNonOwner_throwsAccessDenied() {
        when(postRepository.findById(10L)).thenReturn(Optional.of(premiumPost(10L, creator(1L), tier(5L, creator(1L)))));
        PostRequest req = PostRequest.builder().title("hack").premium(true).tierId(5L).build();
        assertThrows(AccessDeniedException.class, () -> postService.update(10L, req, viewer(999L)));
    }

    @Test
    void delete_byOwner_deletes() {
        Post post = premiumPost(10L, creator(1L), tier(5L, creator(1L)));
        when(postRepository.findById(10L)).thenReturn(Optional.of(post));
        postService.delete(10L, viewer(1L));
        verify(postRepository).delete(post);
    }

    @Test
    void delete_byNonOwner_throwsAccessDenied() {
        when(postRepository.findById(10L)).thenReturn(Optional.of(premiumPost(10L, creator(1L), tier(5L, creator(1L)))));
        assertThrows(AccessDeniedException.class, () -> postService.delete(10L, viewer(999L)));
        verify(postRepository, never()).delete(any());
    }
}
