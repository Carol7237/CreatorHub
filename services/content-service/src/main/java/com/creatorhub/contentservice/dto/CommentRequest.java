package com.creatorhub.contentservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Create/update input for a comment. The author is NOT in the body — it comes
 * from the authenticated user.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentRequest {

    @NotBlank(message = "Comment text is required")
    @Size(max = 1000, message = "Comment must be at most 1000 characters")
    private String text;

    @NotNull(message = "postId is required")
    private Long postId;
}
