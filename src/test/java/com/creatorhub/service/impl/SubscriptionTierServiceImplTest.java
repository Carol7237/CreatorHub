package com.creatorhub.service.impl;

import com.creatorhub.common.Viewer;
import com.creatorhub.dto.SubscriptionTierRequest;
import com.creatorhub.dto.SubscriptionTierResponse;
import com.creatorhub.exception.BusinessRuleException;
import com.creatorhub.exception.ResourceInUseException;
import com.creatorhub.exception.ResourceNotFoundException;
import com.creatorhub.model.Post;
import com.creatorhub.model.Subscription;
import com.creatorhub.model.SubscriptionTier;
import com.creatorhub.model.User;
import com.creatorhub.repository.SubscriptionTierRepository;
import com.creatorhub.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionTierServiceImplTest {

    @Mock private SubscriptionTierRepository tierRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private SubscriptionTierServiceImpl tierService;

    private static User creator(Long id) {
        User u = new User();
        u.setId(id);
        u.setUsername("creator" + id);
        return u;
    }

    private static SubscriptionTier tier(Long id, Long creatorId) {
        SubscriptionTier t = new SubscriptionTier();
        t.setId(id);
        t.setName("VIP");
        t.setPriceMonthly(new BigDecimal("9.99"));
        t.setCreator(creator(creatorId));
        return t;
    }

    private SubscriptionTierRequest request(String price) {
        return SubscriptionTierRequest.builder().name("VIP").priceMonthly(new BigDecimal(price)).build();
    }

    private static Viewer viewer(Long id) {
        return new Viewer(id, false);
    }

    @Test
    void create_valid_succeeds() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(creator(1L)));
        when(tierRepository.save(any(SubscriptionTier.class))).thenAnswer(inv -> {
            SubscriptionTier t = inv.getArgument(0);
            t.setId(5L);
            return t;
        });
        SubscriptionTierResponse response = tierService.create(request("9.99"), viewer(1L));
        assertThat(response.getId()).isEqualTo(5L);
        assertThat(response.getCreatorId()).isEqualTo(1L);
    }

    @Test
    void create_zeroPrice_throws() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(creator(1L)));
        assertThrows(BusinessRuleException.class, () -> tierService.create(request("0"), viewer(1L)));
        verify(tierRepository, never()).save(any());
    }

    @Test
    void create_negativePrice_throws() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(creator(1L)));
        assertThrows(BusinessRuleException.class, () -> tierService.create(request("-5"), viewer(1L)));
    }

    @Test
    void findById_notFound_throws() {
        when(tierRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> tierService.findById(99L));
    }

    @Test
    void update_byOwner_changesFields() {
        when(tierRepository.findById(5L)).thenReturn(Optional.of(tier(5L, 1L)));
        SubscriptionTierResponse response = tierService.update(5L, SubscriptionTierRequest.builder()
                .name("Pro").priceMonthly(new BigDecimal("19.99")).build(), viewer(1L));
        assertThat(response.getName()).isEqualTo("Pro");
        assertThat(response.getPriceMonthly()).isEqualByComparingTo("19.99");
    }

    @Test
    void update_byNonOwner_throwsAccessDenied() {
        when(tierRepository.findById(5L)).thenReturn(Optional.of(tier(5L, 1L)));
        assertThrows(AccessDeniedException.class, () -> tierService.update(5L,
                SubscriptionTierRequest.builder().name("hack").build(), viewer(999L)));
    }

    @Test
    void update_zeroPrice_throws() {
        when(tierRepository.findById(5L)).thenReturn(Optional.of(tier(5L, 1L)));
        assertThrows(BusinessRuleException.class, () -> tierService.update(5L,
                SubscriptionTierRequest.builder().priceMonthly(BigDecimal.ZERO).build(), viewer(1L)));
    }

    @Test
    void delete_withSubscriptions_throwsInUse() {
        SubscriptionTier tier = tier(5L, 1L);
        tier.getSubscriptions().add(new Subscription());
        when(tierRepository.findById(5L)).thenReturn(Optional.of(tier));
        assertThrows(ResourceInUseException.class, () -> tierService.delete(5L, viewer(1L)));
        verify(tierRepository, never()).delete(any());
    }

    @Test
    void delete_withGatedPosts_throwsInUse() {
        SubscriptionTier tier = tier(5L, 1L);
        tier.getPosts().add(new Post());
        when(tierRepository.findById(5L)).thenReturn(Optional.of(tier));
        assertThrows(ResourceInUseException.class, () -> tierService.delete(5L, viewer(1L)));
    }

    @Test
    void delete_byNonOwner_throwsAccessDenied() {
        when(tierRepository.findById(5L)).thenReturn(Optional.of(tier(5L, 1L)));
        assertThrows(AccessDeniedException.class, () -> tierService.delete(5L, viewer(999L)));
    }

    @Test
    void delete_byOwner_clean_deletes() {
        SubscriptionTier tier = tier(5L, 1L);
        when(tierRepository.findById(5L)).thenReturn(Optional.of(tier));
        tierService.delete(5L, viewer(1L));
        verify(tierRepository).delete(tier);
    }

    @Test
    void findAll_mapsEntities() {
        when(tierRepository.findAll()).thenReturn(List.of(tier(5L, 1L), tier(6L, 1L)));
        assertThat(tierService.findAll()).hasSize(2);
    }

    @Test
    void findByCreator_delegates() {
        when(tierRepository.findByCreatorId(1L)).thenReturn(List.of(tier(5L, 1L)));
        assertThat(tierService.findByCreator(1L)).hasSize(1);
    }
}
