package com.creatorhub.notificationservice.model;

/**
 * What happened, from the recipient's point of view.
 */
public enum NotificationType {
    /** A fan subscribed to one of the recipient's tiers. */
    NEW_SUBSCRIBER,
    /** Someone commented on one of the recipient's posts. */
    NEW_COMMENT
}
