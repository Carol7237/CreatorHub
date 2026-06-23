package com.creatorhub.contentservice.service;

import com.creatorhub.common.Viewer;
import com.creatorhub.contentservice.dto.CommentRequest;
import com.creatorhub.contentservice.dto.CommentResponse;

import java.util.List;

public interface CommentService {

    CommentResponse create(CommentRequest request, Viewer viewer);

    List<CommentResponse> findByPost(Long postId);

    CommentResponse update(Long id, CommentRequest request, Viewer viewer);

    void delete(Long id, Viewer viewer);
}
