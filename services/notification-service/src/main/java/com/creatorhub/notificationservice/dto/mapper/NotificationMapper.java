package com.creatorhub.notificationservice.dto.mapper;

import com.creatorhub.notificationservice.dto.NotificationResponse;
import com.creatorhub.notificationservice.model.Notification;

public final class NotificationMapper {

    private NotificationMapper() {
    }

    public static NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .recipientId(n.getRecipientId())
                .type(n.getType())
                .message(n.getMessage())
                .actorId(n.getActorId())
                .relatedId(n.getRelatedId())
                .read(n.isRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
