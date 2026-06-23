package com.creatorhub.service.impl;

import com.creatorhub.common.PageableUtils;
import com.creatorhub.common.Viewer;
import com.creatorhub.dto.PagedResponse;
import com.creatorhub.dto.SubscriptionRequest;
import com.creatorhub.dto.SubscriptionResponse;
import com.creatorhub.dto.mapper.SubscriptionMapper;
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
import com.creatorhub.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class SubscriptionServiceImpl implements SubscriptionService {

    private static final Set<String> ALLOWED_SORT = Set.of("id", "startDate", "status");

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final SubscriptionTierRepository tierRepository;

    @Override
    public SubscriptionResponse create(SubscriptionRequest request, Viewer viewer) {
        User fan = userRepository.findById(viewer.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User", viewer.userId()));
        SubscriptionTier tier = tierRepository.findById(request.getTierId())
                .orElseThrow(() -> new ResourceNotFoundException("SubscriptionTier", request.getTierId()));

        // A creator cannot subscribe to their own tier.
        if (tier.getCreator() != null && tier.getCreator().getId().equals(fan.getId())) {
            log.warn("Rejected self-subscription: user={} tier={}", fan.getId(), tier.getId());
            throw new BusinessRuleException("A creator cannot subscribe to their own tier");
        }

        // Cannot have two active subscriptions to the same tier.
        if (subscriptionRepository.existsByFanIdAndTierIdAndStatus(fan.getId(), tier.getId(), SubStatus.ACTIVE)) {
            log.warn("Rejected duplicate active subscription: fan={} tier={}", fan.getId(), tier.getId());
            throw new DuplicateResourceException(
                    "Fan " + fan.getId() + " already has an active subscription to tier " + tier.getId());
        }

        Subscription subscription = new Subscription();
        subscription.setFan(fan);
        subscription.setTier(tier);
        subscription.setStatus(SubStatus.ACTIVE);
        subscription.setStartDate(LocalDate.now());

        Subscription saved = subscriptionRepository.save(subscription);
        log.info("Subscription activated: id={} fan={} tier={}", saved.getId(), fan.getId(), tier.getId());
        return SubscriptionMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public SubscriptionResponse findById(Long id) {
        return SubscriptionMapper.toResponse(getOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubscriptionResponse> findAll() {
        return subscriptionRepository.findAll().stream().map(SubscriptionMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<SubscriptionResponse> findAll(Pageable pageable) {
        Pageable safe = PageableUtils.sanitize(pageable, ALLOWED_SORT);
        return PagedResponse.from(subscriptionRepository.findAll(safe).map(SubscriptionMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubscriptionResponse> findByFan(Long fanId) {
        return subscriptionRepository.findByFanId(fanId).stream()
                .map(SubscriptionMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<SubscriptionResponse> findByFan(Long fanId, Pageable pageable) {
        Pageable safe = PageableUtils.sanitize(pageable, ALLOWED_SORT);
        return PagedResponse.from(subscriptionRepository.findByFanId(fanId, safe).map(SubscriptionMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubscriptionResponse> findByTier(Long tierId) {
        return subscriptionRepository.findByTierId(tierId).stream()
                .map(SubscriptionMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubscriptionResponse> findByFanAndStatus(Long fanId, SubStatus status) {
        return subscriptionRepository.findByFanIdAndStatus(fanId, status).stream()
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

    private void assertOwnerOrAdmin(Subscription subscription, Viewer viewer) {
        Long ownerId = subscription.getFan() != null ? subscription.getFan().getId() : null;
        if (!viewer.isOwnerOrAdmin(ownerId)) {
            throw new AccessDeniedException("You can only manage your own subscriptions");
        }
    }

    private Subscription getOrThrow(Long id) {
        return subscriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", id));
    }
}
