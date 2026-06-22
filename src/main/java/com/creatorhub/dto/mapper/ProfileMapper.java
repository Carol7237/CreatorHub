package com.creatorhub.dto.mapper;

import com.creatorhub.dto.ProfileResponse;
import com.creatorhub.model.Profile;

public final class ProfileMapper {

    private ProfileMapper() {
    }

    public static ProfileResponse toResponse(Profile profile) {
        return ProfileResponse.builder()
                .id(profile.getId())
                .displayName(profile.getDisplayName())
                .bio(profile.getBio())
                .avatarUrl(profile.getAvatarUrl())
                .userId(profile.getUser() != null ? profile.getUser().getId() : null)
                .build();
    }
}
