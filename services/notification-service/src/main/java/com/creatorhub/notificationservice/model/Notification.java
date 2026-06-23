package com.creatorhub.notificationservice.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * A notification document (MongoDB). Simple, relation-free — a natural NoSQL fit.
 * Cross-service references (recipient/actor users, the related post/subscription)
 * are stored as plain ids, never FKs.
 */
@Document(collection = "notifications")
@Getter
@Setter
@ToString
public class Notification {

    @Id
    private String id;

    /** The user (User service) who receives this notification. */
    @Indexed
    private Long recipientId;

    private NotificationType type;

    private String message;

    /** Who triggered it (the subscriber / commenter). */
    private Long actorId;

    /** Context id: the post (NEW_COMMENT) or tier (NEW_SUBSCRIBER) involved. */
    private Long relatedId;

    private boolean read = false;

    private Instant createdAt = Instant.now();
}
