package com.creatorhub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Input for creating a subscription. startDate and status are set by the service
 * (today / ACTIVE), not supplied by the caller.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionRequest {

    private Long fanId;
    private Long tierId;
}
