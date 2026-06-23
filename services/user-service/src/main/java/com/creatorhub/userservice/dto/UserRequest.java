package com.creatorhub.userservice.dto;

import com.creatorhub.userservice.model.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Internal input for creating/updating a user. Carries the plaintext password
 * (BCrypt-encoded by the service). The optional profile fields seed the
 * auto-created {@code Profile}. Public self-registration uses {@link RegisterRequest}.
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
