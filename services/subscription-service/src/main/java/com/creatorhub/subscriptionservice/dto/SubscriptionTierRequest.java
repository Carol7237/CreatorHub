package com.creatorhub.subscriptionservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Create/update input for a subscription tier. The creator is NOT in the body —
 * it comes from the authenticated user (gateway-injected identity).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionTierRequest {

    @NotBlank(message = "Tier name is required")
    @Size(max = 100, message = "Tier name must be at most 100 characters")
    private String name;

    @NotNull(message = "Monthly price is required")
    @DecimalMin(value = "0.01", message = "Monthly price must be greater than 0")
    private BigDecimal priceMonthly;

    @Size(max = 2000, message = "Perks must be at most 2000 characters")
    private String perks;
}
