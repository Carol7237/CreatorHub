package com.creatorhub.service.impl;

import com.creatorhub.common.Viewer;
import com.creatorhub.dto.SubscriptionRequest;
import com.creatorhub.dto.SubscriptionResponse;
import com.creatorhub.exception.BusinessRuleException;
import com.creatorhub.exception.DuplicateResourceException;
import com.creatorhub.exception.ResourceNotFoundException;
import com.creatorhub.model.Subscription;
import com.creatorhub.model.SubscriptionTier;
import com.creatorhub.model.User;
import com.creatorhub.model.enums.SubStatus;
import com.creatorhub.repository.SubscriptionRepository;
import com.creatorhub.repository.SubscriptionTierRepository;
import com.creatorhub.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceImplTest {

    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private UserRepository userRepository;
    @Mock private SubscriptionTierRepository tierRepository;

    @InjectMocks private SubscriptionServiceImpl subscriptionService;

    private static User user(Long id) {
        User u = new User();
        u.setId(id);
        u.setUsername("u" + id);
        return u;
    }

    private static SubscriptionTier tier(Long id, Long creatorId) {
        SubscriptionTier t = new SubscriptionTier();
        t.setId(id);
        t.setName("VIP");
        t.setCreator(user(creatorId));
        return t;
    }

    private static Viewer fan(Long id) {
        return new Viewer(id, false);
    }

    @Test
    void create_valid_setsActiveAndToday() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        when(tierRepository.findById(5L)).thenReturn(Optional.of(tier(5L, 2L)));
        when(subscriptionRepository.existsByFanIdAndTierIdAndStatus(1L, 5L, SubStatus.ACTIVE)).thenReturn(false);
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(inv -> {
            Subscription s = inv.getArgument(0);
            s.setId(10L);
            return s;
        });

        SubscriptionResponse response = subscriptionService.create(
                SubscriptionRequest.builder().tierId(5L).build(), fan(1L));

        assertThat(response.getStatus()).isEqualTo(SubStatus.ACTIVE);
        assertThat(response.getStartDate()).isEqualTo(LocalDate.now());
        assertThat(response.getCreatorId()).isEqualTo(2L);
    }

    @Test
    void create_duplicateActive_throws() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        when(tierRepository.findById(5L)).thenReturn(Optional.of(tier(5L, 2L)));
        when(subscriptionRepository.existsByFanIdAndTierIdAndStatus(1L, 5L, SubStatus.ACTIVE)).thenReturn(true);
        assertThrows(DuplicateResourceException.class, () -> subscriptionService.create(
                SubscriptionRequest.builder().tierId(5L).build(), fan(1L)));
        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    void create_selfSubscription_throws() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        when(tierRepository.findById(5L)).thenReturn(Optional.of(tier(5L, 1L))); // creator == fan
        assertThrows(BusinessRuleException.class, () -> subscriptionService.create(
                SubscriptionRequest.builder().tierId(5L).build(), fan(1L)));
        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    void create_tierNotFound_throws() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        when(tierRepository.findById(5L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> subscriptionService.create(
                SubscriptionRequest.builder().tierId(5L).build(), fan(1L)));
    }

    @Test
    void cancel_byOwner_setsCancelled() {
        Subscription sub = new Subscription();
        sub.setId(10L);
        sub.setFan(user(1L));
        sub.setTier(tier(5L, 2L));
        sub.setStatus(SubStatus.ACTIVE);
        when(subscriptionRepository.findById(10L)).thenReturn(Optional.of(sub));
        SubscriptionResponse response = subscriptionService.cancel(10L, fan(1L));
        assertThat(response.getStatus()).isEqualTo(SubStatus.CANCELLED);
    }

    @Test
    void cancel_byNonOwner_throwsAccessDenied() {
        Subscription sub = new Subscription();
        sub.setId(10L);
        sub.setFan(user(1L));
        sub.setStatus(SubStatus.ACTIVE);
        when(subscriptionRepository.findById(10L)).thenReturn(Optional.of(sub));
        assertThrows(AccessDeniedException.class, () -> subscriptionService.cancel(10L, fan(999L)));
    }

    @Test
    void cancel_alreadyCancelled_throws() {
        Subscription sub = new Subscription();
        sub.setId(10L);
        sub.setFan(user(1L));
        sub.setStatus(SubStatus.CANCELLED);
        when(subscriptionRepository.findById(10L)).thenReturn(Optional.of(sub));
        assertThrows(BusinessRuleException.class, () -> subscriptionService.cancel(10L, fan(1L)));
    }

    @Test
    void delete_byOwner_deletes() {
        Subscription sub = new Subscription();
        sub.setId(10L);
        sub.setFan(user(1L));
        when(subscriptionRepository.findById(10L)).thenReturn(Optional.of(sub));
        subscriptionService.delete(10L, fan(1L));
        verify(subscriptionRepository).delete(sub);
    }
}
