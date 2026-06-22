package com.creatorhub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostRequest {

    private String title;
    private String body;
    private boolean premium;

    /** The creator authoring the post. */
    private Long authorId;

    /** Required when premium=true, must be null when premium=false. */
    private Long tierId;

    /** Optional tag names; resolved get-or-create by the service. */
    private Set<String> tags;
}
