package com.creatorhub.subscriptionservice.dto;

import com.creatorhub.subscriptionservice.model.enums.SubStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Subscription output. {@code tierName} and {@code creatorId} are available locally
 * (the tier lives in this service); the fan's display name lives in the User service.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionResponse {

    private Long id;
    private Long fanId;
    private Long tierId;
    private String tierName;
    private Long creatorId;
    private LocalDate startDate;
    private SubStatus status;
}
