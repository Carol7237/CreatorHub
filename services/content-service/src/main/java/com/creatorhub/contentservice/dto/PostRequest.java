package com.creatorhub.contentservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * Create/update input for a post. SECURITY: the author is NOT in the body — it
 * comes from the authenticated user (gateway-injected identity).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must be at most 200 characters")
    private String title;

    @Size(max = 20000, message = "Body must be at most 20000 characters")
    private String body;

    private boolean premium;

    /** Required when premium=true, must be null when premium=false (checked in the service). */
    private Long tierId;

    /** Optional tag names; resolved get-or-create by the service. */
    private Set<String> tags;
}
