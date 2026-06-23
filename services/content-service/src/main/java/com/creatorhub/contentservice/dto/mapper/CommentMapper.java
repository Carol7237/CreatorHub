package com.creatorhub.contentservice.dto.mapper;

import com.creatorhub.contentservice.dto.CommentResponse;
import com.creatorhub.contentservice.model.Comment;

public final class CommentMapper {

    private CommentMapper() {
    }

    public static CommentResponse toResponse(Comment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .text(comment.getText())
                .createdAt(comment.getCreatedAt())
                .postId(comment.getPost() != null ? comment.getPost().getId() : null)
                .authorId(comment.getAuthorId())
                .build();
    }
}
