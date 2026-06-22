package com.creatorhub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostResponse {

    private Long id;
    private String title;
    private String body;
    private boolean premium;
    private LocalDateTime createdAt;

    private Long creatorId;
    private String creatorUsername;

    /** Null for free posts. */
    private Long tierId;
    private String tierName;

    private Set<String> tags;
}
