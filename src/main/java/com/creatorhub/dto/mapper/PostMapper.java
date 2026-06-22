package com.creatorhub.dto.mapper;

import com.creatorhub.dto.PostResponse;
import com.creatorhub.model.Post;
import com.creatorhub.model.SubscriptionTier;
import com.creatorhub.model.Tag;

import java.util.LinkedHashSet;
import java.util.stream.Collectors;

public final class PostMapper {

    private PostMapper() {
    }

    public static PostResponse toResponse(Post post) {
        SubscriptionTier tier = post.getTier();
        return PostResponse.builder()
                .id(post.getId())
                .title(post.getTitle())
                .body(post.getBody())
                .premium(post.isPremium())
                .createdAt(post.getCreatedAt())
                .creatorId(post.getAuthor() != null ? post.getAuthor().getId() : null)
                .creatorUsername(post.getAuthor() != null ? post.getAuthor().getUsername() : null)
                .tierId(tier != null ? tier.getId() : null)
                .tierName(tier != null ? tier.getName() : null)
                .tags(post.getTags().stream()
                        .map(Tag::getName)
                        .collect(Collectors.toCollection(LinkedHashSet::new)))
                .build();
    }
}
