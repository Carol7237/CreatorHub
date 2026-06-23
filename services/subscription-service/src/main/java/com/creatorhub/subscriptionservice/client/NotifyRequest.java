package com.creatorhub.subscriptionservice.client;

/**
 * Payload sent to notification-service's internal endpoint. {@code type} is the
 * notification-type name (e.g. "NEW_SUBSCRIBER"); the notification-service maps it
 * to its own enum.
 */
public record NotifyRequest(Long recipientId, String type, String message, Long actorId, Long relatedId) {
}
