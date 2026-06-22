package com.creatorhub.service;

import com.creatorhub.dto.PagedResponse;
import com.creatorhub.dto.PostRequest;
import com.creatorhub.dto.PostResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PostService {

    PostResponse create(PostRequest request);

    PostResponse findById(Long id);

    List<PostResponse> findAll();

    /** Paginated + sorted (allowed sort: id, title, createdAt, premium). */
    PagedResponse<PostResponse> findAll(Pageable pageable);

    List<PostResponse> findByCreator(Long creatorId);

    /** Paginated + sorted posts of a creator. */
    PagedResponse<PostResponse> findByCreator(Long creatorId, Pageable pageable);

    List<PostResponse> findByPremium(boolean premium);

    List<PostResponse> findByCreatorAndPremium(Long creatorId, boolean premium);

    PostResponse update(Long id, PostRequest request);

    void delete(Long id);
}
