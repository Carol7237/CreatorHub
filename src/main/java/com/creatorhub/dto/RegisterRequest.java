package com.creatorhub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Self-registration input. Deliberately has NO role field: the service always
 * creates a USER, so a caller cannot escalate to ADMIN via registration.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    private String username;
    private String email;
    private String password;

    /** Optional; seeds the auto-created profile (defaults to username). */
    private String displayName;
}
