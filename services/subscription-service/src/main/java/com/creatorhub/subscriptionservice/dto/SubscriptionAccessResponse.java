package com.creatorhub.subscriptionservice.dto;

/**
 * Response of the internal premium-gating contract:
 * {@code GET /internal/subscriptions/access?fanId=&tierId=}. Tells the Content
 * service whether a fan holds an ACTIVE subscription to a tier.
 */
public record SubscriptionAccessResponse(Long fanId, Long tierId, boolean active) {
}
