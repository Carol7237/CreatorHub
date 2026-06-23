package com.creatorhub.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Profile update input (partial — only provided fields are applied), so fields
 * are size-bounded but not @NotBlank.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileRequest {

    @Size(max = 100, message = "Display name must be at most 100 characters")
    private String displayName;

    @Size(max = 1000, message = "Bio must be at most 1000 characters")
    private String bio;

    @Size(max = 500, message = "Avatar URL must be at most 500 characters")
    private String avatarUrl;
}
