package com.creatorhub.contentservice.dto.mapper;

import com.creatorhub.contentservice.dto.PostResponse;
import com.creatorhub.contentservice.model.Post;
import com.creatorhub.contentservice.model.Tag;

import java.util.LinkedHashSet;
import java.util.stream.Collectors;

public final class PostMapper {

    private PostMapper() {
    }

    public static PostResponse toResponse(Post post) {
        return PostResponse.builder()
                .id(post.getId())
                .title(post.getTitle())
                .body(post.getBody())
                .premium(post.isPremium())
                .createdAt(post.getCreatedAt())
                .creatorId(post.getAuthorId())
                .tierId(post.getTierId())
                .tags(post.getTags().stream()
                        .map(Tag::getName)
                        .collect(Collectors.toCollection(LinkedHashSet::new)))
                .build();
    }
}
