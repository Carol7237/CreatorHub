package com.creatorhub.dto;

import com.creatorhub.model.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Input for creating/updating a user. Carries the plaintext password for now
 * (it will be BCrypt-encoded in the Security phase). The optional profile fields
 * seed the auto-created {@code Profile}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRequest {

    private String username;
    private String email;
    private String password;

    /** Optional; defaults to USER when not provided. */
    private Role role;

    /** Optional profile data; displayName defaults to the username. */
    private String displayName;
    private String bio;
    private String avatarUrl;
}
