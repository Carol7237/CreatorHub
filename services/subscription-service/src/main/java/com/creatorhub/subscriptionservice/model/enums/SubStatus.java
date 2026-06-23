package com.creatorhub.subscriptionservice.model.enums;

/**
 * Lifecycle status of a {@code Subscription}.
 * Persisted as a STRING (see {@code @Enumerated(EnumType.STRING)} on the field).
 */
public enum SubStatus {
    ACTIVE,
    CANCELLED,
    EXPIRED
}
