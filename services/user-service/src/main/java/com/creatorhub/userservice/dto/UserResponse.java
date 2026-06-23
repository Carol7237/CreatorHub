package com.creatorhub.userservice.dto;

import com.creatorhub.userservice.model.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Output for a user. SECURITY: never contains the password.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private Long id;
    private String username;
    private String email;
    private Role role;
    private boolean enabled;

    /** Flat reference to the associated profile. */
    private Long profileId;
    private String displayName;
}
