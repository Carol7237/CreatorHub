package com.creatorhub.subscriptionservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Create input for a subscription. The fan is NOT in the body — it comes from the
 * authenticated user. startDate/status are set by the service (today / ACTIVE).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionRequest {

    @NotNull(message = "tierId is required")
    private Long tierId;
}
