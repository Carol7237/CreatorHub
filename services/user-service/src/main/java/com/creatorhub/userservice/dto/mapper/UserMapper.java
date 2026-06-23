package com.creatorhub.userservice.dto.mapper;

import com.creatorhub.userservice.dto.UserResponse;
import com.creatorhub.userservice.model.Profile;
import com.creatorhub.userservice.model.User;

/**
 * Manual entity -> response mapping. Must be invoked inside an open transaction
 * because it touches the lazily-loaded profile.
 */
public final class UserMapper {

    private UserMapper() {
    }

    public static UserResponse toResponse(User user) {
        Profile profile = user.getProfile();
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .enabled(user.isEnabled())
                .profileId(profile != null ? profile.getId() : null)
                .displayName(profile != null ? profile.getDisplayName() : null)
                .build();
    }
}
