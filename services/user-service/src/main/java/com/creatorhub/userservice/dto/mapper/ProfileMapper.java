package com.creatorhub.userservice.dto.mapper;

import com.creatorhub.userservice.dto.ProfileResponse;
import com.creatorhub.userservice.model.Profile;

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
