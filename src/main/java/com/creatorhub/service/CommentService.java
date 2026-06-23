package com.creatorhub.service;

import com.creatorhub.common.Viewer;
import com.creatorhub.dto.CommentRequest;
import com.creatorhub.dto.CommentResponse;

import java.util.List;

public interface CommentService {

    /** Author comes from the viewer; commenting on a premium post requires access (subscriber/author/admin). */
    CommentResponse create(CommentRequest request, Viewer viewer);

    CommentResponse findById(Long id);

    List<CommentResponse> findAll();

    List<CommentResponse> findByPost(Long postId);

    CommentResponse update(Long id, CommentRequest request, Viewer viewer);

    void delete(Long id, Viewer viewer);
}
