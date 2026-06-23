package com.creatorhub.subscriptionservice.service.impl;

import com.creatorhub.common.Viewer;
import com.creatorhub.common.exception.BusinessRuleException;
import com.creatorhub.common.exception.ResourceInUseException;
import com.creatorhub.common.exception.ResourceNotFoundException;
import com.creatorhub.subscriptionservice.dto.SubscriptionTierRequest;
import com.creatorhub.subscriptionservice.dto.SubscriptionTierResponse;
import com.creatorhub.subscriptionservice.model.Subscription;
import com.creatorhub.subscriptionservice.model.SubscriptionTier;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionTierServiceImplTest {

    @Mock private SubscriptionTierRepository tierRepository;
    @InjectMocks private SubscriptionTierServiceImpl tierService;

    private static SubscriptionTier tier(Long id, Long creatorId) {
        SubscriptionTier t = new SubscriptionTier();
        t.setId(id);
        t.setName("VIP");
        t.setPriceMonthly(new BigDecimal("9.99"));
        t.setCreatorId(creatorId);
        return t;
    }

    private static SubscriptionTierRequest request(BigDecimal price) {
        return SubscriptionTierRequest.builder().name("VIP").priceMonthly(price).perks("perks").build();
    }

    @Test
    void create_validPrice_setsCreatorFromViewer() {
        when(tierRepository.save(any(SubscriptionTier.class))).thenAnswer(inv -> {
            SubscriptionTier t = inv.getArgument(0);
            t.setId(1L);
            return t;
        });
        SubscriptionTierResponse resp = tierService.create(request(new BigDecimal("5.00")), new Viewer(42L, false));
        assertThat(resp.getCreatorId()).isEqualTo(42L);
        assertThat(resp.getName()).isEqualTo("VIP");
    }

    @Test
    void create_zeroPrice_throws() {
        assertThrows(BusinessRuleException.class,
                () -> tierService.create(request(BigDecimal.ZERO), new Viewer(42L, false)));
        verify(tierRepository, never()).save(any());
    }

    @Test
    void update_byOwner_changesFields() {
        when(tierRepository.findById(1L)).thenReturn(Optional.of(tier(1L, 42L)));
        SubscriptionTierResponse resp = tierService.update(1L,
                SubscriptionTierRequest.builder().name("Gold").build(), new Viewer(42L, false));
        assertThat(resp.getName()).isEqualTo("Gold");
    }

    @Test
    void update_byNonOwner_throwsAccessDenied() {
        when(tierRepository.findById(1L)).thenReturn(Optional.of(tier(1L, 42L)));
        assertThrows(AccessDeniedException.class, () -> tierService.update(1L,
                SubscriptionTierRequest.builder().name("hack").build(), new Viewer(999L, false)));
    }

    @Test
    void delete_withSubscriptions_throwsInUse() {
        SubscriptionTier t = tier(1L, 42L);
        t.getSubscriptions().add(new Subscription());
        when(tierRepository.findById(1L)).thenReturn(Optional.of(t));
        assertThrows(ResourceInUseException.class, () -> tierService.delete(1L, new Viewer(42L, false)));
        verify(tierRepository, never()).delete(any());
    }

    @Test
    void delete_clean_deletes() {
        SubscriptionTier t = tier(1L, 42L);
        when(tierRepository.findById(1L)).thenReturn(Optional.of(t));
        tierService.delete(1L, new Viewer(42L, false));
        verify(tierRepository).delete(t);
    }

    @Test
    void findById_notFound_throws() {
        when(tierRepository.findById(9L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> tierService.findById(9L));
    }
}
