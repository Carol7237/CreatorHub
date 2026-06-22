package com.creatorhub.dto.mapper;

import com.creatorhub.dto.CommentResponse;
import com.creatorhub.model.Comment;

public final class CommentMapper {

    private CommentMapper() {
    }

    public static CommentResponse toResponse(Comment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .text(comment.getText())
                .createdAt(comment.getCreatedAt())
                .postId(comment.getPost() != null ? comment.getPost().getId() : null)
                .authorId(comment.getAuthor() != null ? comment.getAuthor().getId() : null)
                .authorUsername(comment.getAuthor() != null ? comment.getAuthor().getUsername() : null)
                .build();
    }
}
