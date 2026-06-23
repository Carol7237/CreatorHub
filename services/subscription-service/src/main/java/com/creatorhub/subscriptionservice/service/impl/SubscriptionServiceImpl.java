package com.creatorhub.subscriptionservice.service.impl;

import com.creatorhub.common.Viewer;
import com.creatorhub.common.exception.BusinessRuleException;
import com.creatorhub.common.exception.DuplicateResourceException;
import com.creatorhub.common.exception.ResourceNotFoundException;
import com.creatorhub.subscriptionservice.dto.SubscriptionRequest;
import com.creatorhub.subscriptionservice.dto.SubscriptionResponse;
import com.creatorhub.subscriptionservice.dto.mapper.SubscriptionMapper;
import com.creatorhub.subscriptionservice.model.Subscription;
import com.creatorhub.subscriptionservice.model.SubscriptionTier;
import com.creatorhub.subscriptionservice.model.enums.SubStatus;
import com.creatorhub.subscriptionservice.repository.SubscriptionRepository;
import com.creatorhub.subscriptionservice.repository.SubscriptionTierRepository;
import com.creatorhub.subscriptionservice.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class SubscriptionServiceImpl implements SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionTierRepository tierRepository;

    @Override
    public SubscriptionResponse create(SubscriptionRequest request, Viewer viewer) {
        Long fanId = viewer.userId();
        SubscriptionTier tier = tierRepository.findById(request.getTierId())
                .orElseThrow(() -> new ResourceNotFoundException("SubscriptionTier", request.getTierId()));

        // A creator cannot subscribe to their own tier (both are user ids now).
        if (fanId.equals(tier.getCreatorId())) {
            log.warn("Rejected self-subscription: user={} tier={}", fanId, tier.getId());
            throw new BusinessRuleException("A creator cannot subscribe to their own tier");
        }

        // Cannot have two active subscriptions to the same tier.
        if (subscriptionRepository.existsByFanIdAndTierIdAndStatus(fanId, tier.getId(), SubStatus.ACTIVE)) {
            log.warn("Rejected duplicate active subscription: fan={} tier={}", fanId, tier.getId());
            throw new DuplicateResourceException(
                    "Fan " + fanId + " already has an active subscription to tier " + tier.getId());
        }

        Subscription subscription = new Subscription();
        subscription.setFanId(fanId);
        subscription.setTier(tier);
        subscription.setStatus(SubStatus.ACTIVE);
        subscription.setStartDate(LocalDate.now());

        Subscription saved = subscriptionRepository.save(subscription);
        log.info("Subscription activated: id={} fan={} tier={}", saved.getId(), fanId, tier.getId());
        return SubscriptionMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubscriptionResponse> findByFan(Long fanId) {
        return subscriptionRepository.findByFanId(fanId).stream()
                .map(SubscriptionMapper::toResponse).toList();
    }

    @Override
    public SubscriptionResponse cancel(Long id, Viewer viewer) {
        Subscription subscription = getOrThrow(id);
        assertOwnerOrAdmin(subscription, viewer);
        if (subscription.getStatus() == SubStatus.CANCELLED) {
            throw new BusinessRuleException("Subscription " + id + " is already cancelled");
        }
        subscription.setStatus(SubStatus.CANCELLED);
        log.info("Subscription cancelled: id={}", id);
        return SubscriptionMapper.toResponse(subscription);
    }

    @Override
    public void delete(Long id, Viewer viewer) {
        Subscription subscription = getOrThrow(id);
        assertOwnerOrAdmin(subscription, viewer);
        subscriptionRepository.delete(subscription);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasActiveAccess(Long fanId, Long tierId) {
        if (fanId == null || tierId == null) {
            return false;
        }
        return subscriptionRepository.existsByFanIdAndTierIdAndStatus(fanId, tierId, SubStatus.ACTIVE);
    }

    private void assertOwnerOrAdmin(Subscription subscription, Viewer viewer) {
        if (!viewer.isOwnerOrAdmin(subscription.getFanId())) {
            throw new AccessDeniedException("You can only manage your own subscriptions");
        }
    }

    private Subscription getOrThrow(Long id) {
        return subscriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", id));
    }
}
