package com.creatorhub.contentservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TagRequest {

    @NotBlank(message = "Tag name is required")
    @Size(max = 50, message = "Tag name must be at most 50 characters")
    private String name;
}
