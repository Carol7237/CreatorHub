package com.creatorhub.notificationservice.dto;

import com.creatorhub.notificationservice.model.NotificationType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Internal contract used by other services (subscription/content) to create a
 * notification on an event. Not exposed through the gateway.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateNotificationRequest {

    @NotNull(message = "recipientId is required")
    private Long recipientId;

    @NotNull(message = "type is required")
    private NotificationType type;

    private String message;
    private Long actorId;
    private Long relatedId;
}
