package com.creatorhub.service;

import com.creatorhub.dto.CommentRequest;
import com.creatorhub.dto.CommentResponse;

import java.util.List;

public interface CommentService {

    CommentResponse create(CommentRequest request);

    CommentResponse findById(Long id);

    List<CommentResponse> findAll();

    List<CommentResponse> findByPost(Long postId);

    CommentResponse update(Long id, CommentRequest request);

    void delete(Long id);
}
