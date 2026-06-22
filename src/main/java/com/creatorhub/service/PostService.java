package com.creatorhub.service;

import com.creatorhub.dto.PostRequest;
import com.creatorhub.dto.PostResponse;

import java.util.List;

public interface PostService {

    PostResponse create(PostRequest request);

    PostResponse findById(Long id);

    List<PostResponse> findAll();

    List<PostResponse> findByCreator(Long creatorId);

    List<PostResponse> findByPremium(boolean premium);

    List<PostResponse> findByCreatorAndPremium(Long creatorId, boolean premium);

    PostResponse update(Long id, PostRequest request);

    void delete(Long id);
}
