package com.creatorhub.service.impl;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class SubscriptionTierServiceImpl implements SubscriptionTierService {

    private final SubscriptionTierRepository tierRepository;
    private final UserRepository userRepository;

    @Override
    public SubscriptionTierResponse create(SubscriptionTierRequest request) {
        User creator = userRepository.findById(request.getCreatorId())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.getCreatorId()));
        validatePrice(request.getPriceMonthly());

        SubscriptionTier tier = new SubscriptionTier();
        tier.setName(request.getName());
        tier.setPriceMonthly(request.getPriceMonthly());
        tier.setPerks(request.getPerks());
        tier.setCreator(creator);

        return SubscriptionTierMapper.toResponse(tierRepository.save(tier));
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
    public SubscriptionTierResponse update(Long id, SubscriptionTierRequest request) {
        SubscriptionTier tier = getOrThrow(id);
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
    public void delete(Long id) {
        SubscriptionTier tier = getOrThrow(id);
        if (!tier.getSubscriptions().isEmpty() || !tier.getPosts().isEmpty()) {
            throw new ResourceInUseException("Cannot delete tier " + id
                    + ": it still has subscriptions or gated posts.");
        }
        tierRepository.delete(tier);
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
