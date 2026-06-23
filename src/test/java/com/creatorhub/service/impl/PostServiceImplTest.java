package com.creatorhub.service.impl;

import com.creatorhub.dto.PostRequest;
import com.creatorhub.dto.PostResponse;
import com.creatorhub.exception.BusinessRuleException;
import com.creatorhub.exception.ResourceNotFoundException;
import com.creatorhub.model.Post;
import com.creatorhub.model.SubscriptionTier;
import com.creatorhub.model.Tag;
import com.creatorhub.model.User;
import com.creatorhub.model.enums.Role;
import com.creatorhub.repository.PostRepository;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostServiceImplTest {

    @Mock private PostRepository postRepository;
    @Mock private UserRepository userRepository;
    @Mock private SubscriptionTierRepository tierRepository;
    @Mock private TagRepository tagRepository;

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

    @Test
    void create_premiumWithoutTier_throws() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(creator(1L)));
        PostRequest req = PostRequest.builder().title("t").body("b").premium(true).authorId(1L).build();
        assertThrows(BusinessRuleException.class, () -> postService.create(req));
        verify(postRepository, never()).save(any());
    }

    @Test
    void create_freeWithTier_throws() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(creator(1L)));
        PostRequest req = PostRequest.builder().title("t").body("b").premium(false).authorId(1L).tierId(5L).build();
        assertThrows(BusinessRuleException.class, () -> postService.create(req));
        verify(postRepository, never()).save(any());
    }

    @Test
    void create_premiumWithForeignTier_throws() {
        User author = creator(1L);
        SubscriptionTier foreignTier = tier(5L, creator(2L)); // owned by a different creator
        when(userRepository.findById(1L)).thenReturn(Optional.of(author));
        when(tierRepository.findById(5L)).thenReturn(Optional.of(foreignTier));

        PostRequest req = PostRequest.builder().title("t").body("b").premium(true).authorId(1L).tierId(5L).build();
        assertThrows(BusinessRuleException.class, () -> postService.create(req));
        verify(postRepository, never()).save(any());
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
                PostRequest.builder().title("Hello").body("b").premium(false).authorId(1L).build());

        assertThat(response.isPremium()).isFalse();
        assertThat(response.getTierId()).isNull();
        assertThat(response.getCreatorId()).isEqualTo(1L);
    }

    @Test
    void create_validPremiumPost_withGetOrCreateTags_succeeds() {
        User author = creator(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(author));
        when(tierRepository.findById(5L)).thenReturn(Optional.of(tier(5L, author)));
        // "news" already exists, "fresh" must be created
        Tag existing = new Tag();
        existing.setId(7L);
        existing.setName("news");
        when(tagRepository.findByNameIn(any())).thenReturn(List.of(existing));
        when(tagRepository.save(any(Tag.class))).thenAnswer(inv -> inv.getArgument(0));
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> {
            Post p = inv.getArgument(0);
            p.setId(100L);
            return p;
        });

        PostResponse response = postService.create(PostRequest.builder()
                .title("Premium").body("b").premium(true).authorId(1L).tierId(5L)
                .tags(Set.of("news", "fresh")).build());

        assertThat(response.isPremium()).isTrue();
        assertThat(response.getTierId()).isEqualTo(5L);
        assertThat(response.getTags()).containsExactlyInAnyOrder("news", "fresh");
        verify(tagRepository, times(1)).save(any(Tag.class)); // only "fresh" is new
    }

    @Test
    void findById_notFound_throws() {
        when(postRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> postService.findById(99L));
    }

    @Test
    void create_authorNotFound_throws() {
        when(userRepository.findById(42L)).thenReturn(Optional.empty());
        PostRequest req = PostRequest.builder().title("t").body("b").premium(false).authorId(42L).build();
        assertThrows(ResourceNotFoundException.class, () -> postService.create(req));
    }

    private static Post post(Long id, User author) {
        Post p = new Post();
        p.setId(id);
        p.setTitle("Post " + id);
        p.setAuthor(author);
        return p;
    }

    @Test
    void findAll_mapsEntities() {
        when(postRepository.findAll()).thenReturn(List.of(post(1L, creator(1L)), post(2L, creator(1L))));
        assertThat(postService.findAll()).hasSize(2);
    }

    @Test
    void findByPremium_delegates() {
        when(postRepository.findByPremium(true)).thenReturn(List.of(post(1L, creator(1L))));
        assertThat(postService.findByPremium(true)).hasSize(1);
    }

    @Test
    void findByCreatorAndPremium_delegates() {
        when(postRepository.findByAuthorIdAndPremium(1L, false)).thenReturn(List.of(post(1L, creator(1L))));
        assertThat(postService.findByCreatorAndPremium(1L, false)).hasSize(1);
    }

    @Test
    void findAll_paged_returnsPagedResponse() {
        Pageable pageable = PageRequest.of(0, 10);
        when(postRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(post(1L, creator(1L))), pageable, 1));
        var page = postService.findAll(pageable);
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getTotalElements()).isEqualTo(1);
    }

    @Test
    void update_freePost_changesTitle() {
        Post existing = post(100L, creator(1L));
        existing.setPremium(false);
        when(postRepository.findById(100L)).thenReturn(Optional.of(existing));

        PostResponse response = postService.update(100L,
                PostRequest.builder().title("Updated title").premium(false).build());

        assertThat(response.getTitle()).isEqualTo("Updated title");
        assertThat(existing.getTitle()).isEqualTo("Updated title");
    }

    @Test
    void delete_deletes() {
        Post existing = post(100L, creator(1L));
        when(postRepository.findById(100L)).thenReturn(Optional.of(existing));
        postService.delete(100L);
        verify(postRepository).delete(existing);
    }
}
