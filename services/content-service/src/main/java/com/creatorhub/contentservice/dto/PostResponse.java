package com.creatorhub.contentservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Post output. Null fields are omitted (e.g. the body of a locked premium post,
 * or tierId on a free post). Display names (creator username, tier name) live in
 * other services and are intentionally not joined here.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PostResponse {

    private Long id;
    private String title;
    private String body;
    private boolean premium;

    /** True when this is a premium post the current viewer cannot access; body is then null. */
    private boolean locked;

    private LocalDateTime createdAt;

    private Long creatorId;

    /** Null for free posts. */
    private Long tierId;

    private Set<String> tags;
}
