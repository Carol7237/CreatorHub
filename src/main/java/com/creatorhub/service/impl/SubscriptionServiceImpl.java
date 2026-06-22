package com.creatorhub.service.impl;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class SubscriptionServiceImpl implements SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final SubscriptionTierRepository tierRepository;

    @Override
    public SubscriptionResponse create(SubscriptionRequest request) {
        User fan = userRepository.findById(request.getFanId())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.getFanId()));
        SubscriptionTier tier = tierRepository.findById(request.getTierId())
                .orElseThrow(() -> new ResourceNotFoundException("SubscriptionTier", request.getTierId()));

        // A creator cannot subscribe to their own tier.
        if (tier.getCreator() != null && tier.getCreator().getId().equals(fan.getId())) {
            throw new BusinessRuleException("A creator cannot subscribe to their own tier");
        }

        // Cannot have two active subscriptions to the same tier.
        if (subscriptionRepository.existsByFanIdAndTierIdAndStatus(
                fan.getId(), tier.getId(), SubStatus.ACTIVE)) {
            throw new DuplicateResourceException(
                    "Fan " + fan.getId() + " already has an active subscription to tier " + tier.getId());
        }

        Subscription subscription = new Subscription();
        subscription.setFan(fan);
        subscription.setTier(tier);
        subscription.setStatus(SubStatus.ACTIVE);
        subscription.setStartDate(LocalDate.now());

        return SubscriptionMapper.toResponse(subscriptionRepository.save(subscription));
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
    public List<SubscriptionResponse> findByFan(Long fanId) {
        return subscriptionRepository.findByFanId(fanId).stream()
                .map(SubscriptionMapper::toResponse).toList();
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
    public SubscriptionResponse cancel(Long id) {
        Subscription subscription = getOrThrow(id);
        if (subscription.getStatus() == SubStatus.CANCELLED) {
            throw new BusinessRuleException("Subscription " + id + " is already cancelled");
        }
        subscription.setStatus(SubStatus.CANCELLED);
        return SubscriptionMapper.toResponse(subscription);
    }

    @Override
    public void delete(Long id) {
        Subscription subscription = getOrThrow(id);
        subscriptionRepository.delete(subscription);
    }

    private Subscription getOrThrow(Long id) {
        return subscriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", id));
    }
}
