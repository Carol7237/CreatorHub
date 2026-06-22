package com.creatorhub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionTierRequest {

    private String name;
    private BigDecimal priceMonthly;
    private String perks;

    /** The creator offering this tier. */
    private Long creatorId;
}
