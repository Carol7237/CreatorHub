package com.creatorhub.service;

import com.creatorhub.common.Viewer;
import com.creatorhub.dto.PagedResponse;
import com.creatorhub.dto.PostRequest;
import com.creatorhub.dto.PostResponse;
import org.springframework.data.domain.Pageable;

/**
 * Reads are viewer-aware: premium posts the viewer cannot access are returned
 * locked (body null, locked=true). Writes use the viewer as owner / for the
 * ownership check.
 */
public interface PostService {

    PostResponse create(PostRequest request, Viewer viewer);

    PostResponse findById(Long id, Viewer viewer);

    /** Paginated + sorted (allowed sort: id, title, createdAt, premium). */
    PagedResponse<PostResponse> findAll(Pageable pageable, Viewer viewer);

    /** Paginated + sorted posts of a creator. */
    PagedResponse<PostResponse> findByCreator(Long creatorId, Pageable pageable, Viewer viewer);

    PostResponse update(Long id, PostRequest request, Viewer viewer);

    void delete(Long id, Viewer viewer);
}
