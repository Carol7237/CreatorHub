package com.creatorhub.dto.mapper;

import com.creatorhub.dto.SubscriptionTierResponse;
import com.creatorhub.model.SubscriptionTier;
import com.creatorhub.model.User;

public final class SubscriptionTierMapper {

    private SubscriptionTierMapper() {
    }

    public static SubscriptionTierResponse toResponse(SubscriptionTier tier) {
        User creator = tier.getCreator();
        return SubscriptionTierResponse.builder()
                .id(tier.getId())
                .name(tier.getName())
                .priceMonthly(tier.getPriceMonthly())
                .perks(tier.getPerks())
                .creatorId(creator != null ? creator.getId() : null)
                .creatorUsername(creator != null ? creator.getUsername() : null)
                .build();
    }
}
