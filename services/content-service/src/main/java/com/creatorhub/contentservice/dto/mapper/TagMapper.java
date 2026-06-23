package com.creatorhub.contentservice.dto.mapper;

import com.creatorhub.contentservice.dto.TagResponse;
import com.creatorhub.contentservice.model.Tag;

public final class TagMapper {

    private TagMapper() {
    }

    public static TagResponse toResponse(Tag tag) {
        return TagResponse.builder()
                .id(tag.getId())
                .name(tag.getName())
                .build();
    }
}
