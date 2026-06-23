package com.creatorhub.subscriptionservice.service.impl;

import com.creatorhub.common.Viewer;
import com.creatorhub.common.exception.BusinessRuleException;
import com.creatorhub.common.exception.ResourceInUseException;
import com.creatorhub.common.exception.ResourceNotFoundException;
import com.creatorhub.subscriptionservice.dto.SubscriptionTierRequest;
import com.creatorhub.subscriptionservice.dto.SubscriptionTierResponse;
import com.creatorhub.subscriptionservice.dto.mapper.SubscriptionTierMapper;
import com.creatorhub.subscriptionservice.model.SubscriptionTier;
import com.creatorhub.subscriptionservice.repository.SubscriptionTierRepository;
import com.creatorhub.subscriptionservice.service.SubscriptionTierService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class SubscriptionTierServiceImpl implements SubscriptionTierService {

    private final SubscriptionTierRepository tierRepository;

    @Override
    public SubscriptionTierResponse create(SubscriptionTierRequest request, Viewer viewer) {
        validatePrice(request.getPriceMonthly());

        SubscriptionTier tier = new SubscriptionTier();
        tier.setName(request.getName());
        tier.setPriceMonthly(request.getPriceMonthly());
        tier.setPerks(request.getPerks());
        // Owner from the authenticated context, never the request body.
        tier.setCreatorId(viewer.userId());

        SubscriptionTier saved = tierRepository.save(tier);
        log.info("Tier created: id={} creator={}", saved.getId(), saved.getCreatorId());
        return SubscriptionTierMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public SubscriptionTierResponse findById(Long id) {
        return SubscriptionTierMapper.toResponse(getOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubscriptionTierResponse> findAll() {
        return tierRepository.findAll().stream().map(SubscriptionTierMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubscriptionTierResponse> findByCreator(Long creatorId) {
        return tierRepository.findByCreatorId(creatorId).stream()
                .map(SubscriptionTierMapper::toResponse).toList();
    }

    @Override
    public SubscriptionTierResponse update(Long id, SubscriptionTierRequest request, Viewer viewer) {
        SubscriptionTier tier = getOrThrow(id);
        assertOwnerOrAdmin(tier, viewer);
        // The owning creator is immutable; only descriptive fields and price change.
        if (request.getName() != null) {
            tier.setName(request.getName());
        }
        if (request.getPriceMonthly() != null) {
            validatePrice(request.getPriceMonthly());
            tier.setPriceMonthly(request.getPriceMonthly());
        }
        if (request.getPerks() != null) {
            tier.setPerks(request.getPerks());
        }
        return SubscriptionTierMapper.toResponse(tier);
    }

    @Override
    public void delete(Long id, Viewer viewer) {
        SubscriptionTier tier = getOrThrow(id);
        assertOwnerOrAdmin(tier, viewer);
        // Block if subscriptions still reference the tier. The monolith also blocked
        // on gated posts, but posts now live in the Content service (cross-service) —
        // that check is deferred (inter-service call/event) to a later step.
        if (!tier.getSubscriptions().isEmpty()) {
            throw new ResourceInUseException("Cannot delete tier " + id + ": it still has subscriptions.");
        }
        tierRepository.delete(tier);
        log.info("Tier deleted: id={}", id);
    }

    private void assertOwnerOrAdmin(SubscriptionTier tier, Viewer viewer) {
        if (!viewer.isOwnerOrAdmin(tier.getCreatorId())) {
            throw new AccessDeniedException("You can only modify your own tiers");
        }
    }

    private void validatePrice(BigDecimal price) {
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("Tier priceMonthly must be greater than 0");
        }
    }

    private SubscriptionTier getOrThrow(Long id) {
        return tierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SubscriptionTier", id));
    }
}
