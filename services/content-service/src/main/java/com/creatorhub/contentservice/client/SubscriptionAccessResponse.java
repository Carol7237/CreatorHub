package com.creatorhub.contentservice.client;

/**
 * Content service's view of the Subscription service's gating contract response
 * ({@code GET /internal/subscriptions/access}). Only {@code active} is needed here;
 * extra JSON fields are ignored by Jackson.
 */
public record SubscriptionAccessResponse(boolean active) {
}
