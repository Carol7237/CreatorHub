package com.creatorhub.dto.mapper;

import com.creatorhub.dto.SubscriptionResponse;
import com.creatorhub.model.Subscription;
import com.creatorhub.model.SubscriptionTier;

public final class SubscriptionMapper {

    private SubscriptionMapper() {
    }

    public static SubscriptionResponse toResponse(Subscription subscription) {
        SubscriptionTier tier = subscription.getTier();
        return SubscriptionResponse.builder()
                .id(subscription.getId())
                .fanId(subscription.getFan() != null ? subscription.getFan().getId() : null)
                .fanUsername(subscription.getFan() != null ? subscription.getFan().getUsername() : null)
                .tierId(tier != null ? tier.getId() : null)
                .tierName(tier != null ? tier.getName() : null)
                .creatorId(tier != null && tier.getCreator() != null ? tier.getCreator().getId() : null)
                .startDate(subscription.getStartDate())
                .status(subscription.getStatus())
                .build();
    }
}
