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
public class SubscriptionTierResponse {

    private Long id;
    private String name;
    private BigDecimal priceMonthly;
    private String perks;
    private Long creatorId;
    private String creatorUsername;
}
