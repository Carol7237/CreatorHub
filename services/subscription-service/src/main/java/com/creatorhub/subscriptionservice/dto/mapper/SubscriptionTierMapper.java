package com.creatorhub.subscriptionservice.dto.mapper;

import com.creatorhub.subscriptionservice.dto.SubscriptionTierResponse;
import com.creatorhub.subscriptionservice.model.SubscriptionTier;

public final class SubscriptionTierMapper {

    private SubscriptionTierMapper() {
    }

    public static SubscriptionTierResponse toResponse(SubscriptionTier tier) {
        return SubscriptionTierResponse.builder()
                .id(tier.getId())
                .name(tier.getName())
                .priceMonthly(tier.getPriceMonthly())
                .perks(tier.getPerks())
                .creatorId(tier.getCreatorId())
                .build();
    }
}
