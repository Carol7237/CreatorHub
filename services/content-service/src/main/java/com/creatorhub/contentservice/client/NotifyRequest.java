package com.creatorhub.contentservice.client;

/**
 * Payload sent to notification-service's internal endpoint. {@code type} is the
 * notification-type name (e.g. "NEW_COMMENT"); notification-service maps it to its enum.
 */
public record NotifyRequest(Long recipientId, String type, String message, Long actorId, Long relatedId) {
}
