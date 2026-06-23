package com.creatorhub.common;

import com.creatorhub.common.exception.BusinessRuleException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

/**
 * Sanitizes incoming {@link Pageable}s at the service boundary:
 * <ul>
 *   <li><b>Sort whitelist</b> — only allows sorting by an explicit set of
 *       properties per entity, so a client can never sort by an internal/sensitive
 *       field (e.g. {@code password}) or trigger a {@code PropertyReferenceException}.</li>
 *   <li><b>Max page size</b> — caps the page size so nobody can request a million
 *       rows at once.</li>
 * </ul>
 */
public final class PageableUtils {

    /** Hard cap on page size enforced in the service layer. */
    public static final int MAX_PAGE_SIZE = 100;

    private PageableUtils() {
    }

    public static Pageable sanitize(Pageable pageable, Set<String> allowedSortProperties) {
        for (Sort.Order order : pageable.getSort()) {
            if (!allowedSortProperties.contains(order.getProperty())) {
                throw new BusinessRuleException("Invalid sort property: '" + order.getProperty()
                        + "'. Allowed: " + allowedSortProperties);
            }
        }
        if (pageable.getPageSize() > MAX_PAGE_SIZE) {
            return PageRequest.of(pageable.getPageNumber(), MAX_PAGE_SIZE, pageable.getSort());
        }
        return pageable;
    }
}
