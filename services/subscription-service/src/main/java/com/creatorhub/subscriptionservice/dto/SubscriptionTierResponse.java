package com.creatorhub.subscriptionservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Tier output. Carries {@code creatorId} only (the creator's display name lives in
 * the User service; cross-service display joins are deferred / denormalized later).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionTierResponse {

    private Long id;
    private String name;
    private BigDecimal priceMonthly;
    private String perks;
    private Long creatorId;
}
