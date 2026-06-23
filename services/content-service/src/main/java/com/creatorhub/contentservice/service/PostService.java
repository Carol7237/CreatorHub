package com.creatorhub.contentservice.service;

import com.creatorhub.common.Viewer;
import com.creatorhub.common.dto.PagedResponse;
import com.creatorhub.contentservice.dto.PostRequest;
import com.creatorhub.contentservice.dto.PostResponse;
import org.springframework.data.domain.Pageable;

/**
 * Reads are viewer-aware: premium posts the viewer cannot access are returned
 * locked (body null, locked=true). Access for the "active subscriber" case is
 * resolved by an inter-service call to the Subscription service.
 */
public interface PostService {

    PostResponse create(PostRequest request, Viewer viewer);

    PostResponse findById(Long id, Viewer viewer);

    PagedResponse<PostResponse> findAll(Pageable pageable, Viewer viewer);

    PagedResponse<PostResponse> findByCreator(Long creatorId, Pageable pageable, Viewer viewer);

    PostResponse update(Long id, PostRequest request, Viewer viewer);

    void delete(Long id, Viewer viewer);
}
