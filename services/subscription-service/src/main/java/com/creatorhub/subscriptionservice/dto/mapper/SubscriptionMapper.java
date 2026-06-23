package com.creatorhub.subscriptionservice.dto.mapper;

import com.creatorhub.subscriptionservice.dto.SubscriptionResponse;
import com.creatorhub.subscriptionservice.model.Subscription;
import com.creatorhub.subscriptionservice.model.SubscriptionTier;

public final class SubscriptionMapper {

    private SubscriptionMapper() {
    }

    public static SubscriptionResponse toResponse(Subscription subscription) {
        SubscriptionTier tier = subscription.getTier();
        return SubscriptionResponse.builder()
                .id(subscription.getId())
                .fanId(subscription.getFanId())
                .tierId(tier != null ? tier.getId() : null)
                .tierName(tier != null ? tier.getName() : null)
                .creatorId(tier != null ? tier.getCreatorId() : null)
                .startDate(subscription.getStartDate())
                .status(subscription.getStatus())
                .build();
    }
}
