package com.creatorhub.dto.mapper;

import com.creatorhub.dto.TagResponse;
import com.creatorhub.model.Tag;

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
