package com.creatorhub.subscriptionservice.service.impl;

import com.creatorhub.common.Viewer;
import com.creatorhub.common.exception.BusinessRuleException;
import com.creatorhub.common.exception.DuplicateResourceException;
import com.creatorhub.common.exception.ResourceNotFoundException;
import com.creatorhub.subscriptionservice.dto.SubscriptionRequest;
import com.creatorhub.subscriptionservice.dto.SubscriptionResponse;
import com.creatorhub.subscriptionservice.model.Subscription;
import com.creatorhub.subscriptionservice.model.SubscriptionTier;
import com.creatorhub.subscriptionservice.model.enums.SubStatus;
import com.creatorhub.subscriptionservice.repository.SubscriptionRepository;
import com.creatorhub.subscriptionservice.repository.SubscriptionTierRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceImplTest {

    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private SubscriptionTierRepository tierRepository;
    @InjectMocks private SubscriptionServiceImpl subscriptionService;

    private static SubscriptionTier tier(Long id, Long creatorId) {
        SubscriptionTier t = new SubscriptionTier();
        t.setId(id);
        t.setName("VIP");
        t.setPriceMonthly(new BigDecimal("9.99"));
        t.setCreatorId(creatorId);
        return t;
    }

    @Test
    void create_success_activeSubscription() {
        when(tierRepository.findById(1L)).thenReturn(Optional.of(tier(1L, 100L)));
        when(subscriptionRepository.existsByFanIdAndTierIdAndStatus(7L, 1L, SubStatus.ACTIVE)).thenReturn(false);
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(inv -> {
            Subscription s = inv.getArgument(0);
            s.setId(50L);
            return s;
        });

        SubscriptionResponse resp = subscriptionService.create(
                SubscriptionRequest.builder().tierId(1L).build(), new Viewer(7L, false));

        assertThat(resp.getFanId()).isEqualTo(7L);
        assertThat(resp.getTierId()).isEqualTo(1L);
        assertThat(resp.getStatus()).isEqualTo(SubStatus.ACTIVE);
    }

    @Test
    void create_selfSubscription_throws() {
        when(tierRepository.findById(1L)).thenReturn(Optional.of(tier(1L, 7L))); // creator == fan
        assertThrows(BusinessRuleException.class, () -> subscriptionService.create(
                SubscriptionRequest.builder().tierId(1L).build(), new Viewer(7L, false)));
        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    void create_duplicateActive_throws() {
        when(tierRepository.findById(1L)).thenReturn(Optional.of(tier(1L, 100L)));
        when(subscriptionRepository.existsByFanIdAndTierIdAndStatus(7L, 1L, SubStatus.ACTIVE)).thenReturn(true);
        assertThrows(DuplicateResourceException.class, () -> subscriptionService.create(
                SubscriptionRequest.builder().tierId(1L).build(), new Viewer(7L, false)));
        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    void create_tierNotFound_throws() {
        when(tierRepository.findById(9L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> subscriptionService.create(
                SubscriptionRequest.builder().tierId(9L).build(), new Viewer(7L, false)));
    }

    @Test
    void cancel_byOwner_setsCancelled() {
        Subscription s = new Subscription();
        s.setId(50L);
        s.setFanId(7L);
        s.setTier(tier(1L, 100L));
        s.setStatus(SubStatus.ACTIVE);
        when(subscriptionRepository.findById(50L)).thenReturn(Optional.of(s));

        SubscriptionResponse resp = subscriptionService.cancel(50L, new Viewer(7L, false));
        assertThat(resp.getStatus()).isEqualTo(SubStatus.CANCELLED);
    }

    @Test
    void cancel_byNonOwner_throwsAccessDenied() {
        Subscription s = new Subscription();
        s.setId(50L);
        s.setFanId(7L);
        s.setTier(tier(1L, 100L));
        s.setStatus(SubStatus.ACTIVE);
        when(subscriptionRepository.findById(50L)).thenReturn(Optional.of(s));
        assertThrows(AccessDeniedException.class, () -> subscriptionService.cancel(50L, new Viewer(999L, false)));
    }

    @Test
    void hasActiveAccess_delegatesToRepository() {
        when(subscriptionRepository.existsByFanIdAndTierIdAndStatus(7L, 1L, SubStatus.ACTIVE)).thenReturn(true);
        assertThat(subscriptionService.hasActiveAccess(7L, 1L)).isTrue();

        assertThat(subscriptionService.hasActiveAccess(null, 1L)).isFalse();
        assertThat(subscriptionService.hasActiveAccess(7L, null)).isFalse();
        verify(subscriptionRepository).existsByFanIdAndTierIdAndStatus(eq(7L), eq(1L), eq(SubStatus.ACTIVE));
    }
}
