package com.creatorhub.notificationservice.dto;

import com.creatorhub.notificationservice.model.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

    private String id;
    private Long recipientId;
    private NotificationType type;
    private String message;
    private Long actorId;
    private Long relatedId;
    private boolean read;
    private Instant createdAt;
}
