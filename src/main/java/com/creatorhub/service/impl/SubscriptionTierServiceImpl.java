package com.creatorhub.service.impl;

import com.creatorhub.common.Viewer;
import com.creatorhub.dto.SubscriptionTierRequest;
import com.creatorhub.dto.SubscriptionTierResponse;
import com.creatorhub.dto.mapper.SubscriptionTierMapper;
import com.creatorhub.exception.BusinessRuleException;
import com.creatorhub.exception.ResourceInUseException;
import com.creatorhub.exception.ResourceNotFoundException;
import com.creatorhub.model.SubscriptionTier;
import com.creatorhub.model.User;
import com.creatorhub.repository.SubscriptionTierRepository;
import com.creatorhub.repository.UserRepository;
import com.creatorhub.service.SubscriptionTierService;
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
    private final UserRepository userRepository;

    @Override
    public SubscriptionTierResponse create(SubscriptionTierRequest request, Viewer viewer) {
        User creator = userRepository.findById(viewer.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User", viewer.userId()));
        validatePrice(request.getPriceMonthly());

        SubscriptionTier tier = new SubscriptionTier();
        tier.setName(request.getName());
        tier.setPriceMonthly(request.getPriceMonthly());
        tier.setPerks(request.getPerks());
        tier.setCreator(creator);

        SubscriptionTier saved = tierRepository.save(tier);
        log.info("Tier created: id={} creator={}", saved.getId(), creator.getId());
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
        if (!tier.getSubscriptions().isEmpty() || !tier.getPosts().isEmpty()) {
            throw new ResourceInUseException("Cannot delete tier " + id
                    + ": it still has subscriptions or gated posts.");
        }
        tierRepository.delete(tier);
    }

    private void assertOwnerOrAdmin(SubscriptionTier tier, Viewer viewer) {
        Long ownerId = tier.getCreator() != null ? tier.getCreator().getId() : null;
        if (!viewer.isOwnerOrAdmin(ownerId)) {
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
