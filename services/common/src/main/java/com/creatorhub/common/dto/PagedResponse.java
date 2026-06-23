package com.creatorhub.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Stable, framework-agnostic paginated response. Avoids leaking Spring's
 * {@code Page} internals to API clients.
 *
 * @param <T> element type (always a response DTO, never an entity)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagedResponse<T> {

    private List<T> content;
    private int page;          // 0-based page number
    private int size;          // page size actually used
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;

    /** Wraps a Spring {@link Page} of already-mapped DTOs. */
    public static <T> PagedResponse<T> from(Page<T> page) {
        return PagedResponse.<T>builder()
                .content(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }
}
