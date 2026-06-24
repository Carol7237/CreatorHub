package com.creatorhub.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Login response carrying the signed JWT access token plus the authenticated user.
 * {@code type} is always {@code "Bearer"}; {@code expiresIn} is the token lifetime
 * in seconds.
 *
 * <p>Step 1 (transitional): an HTTP session is still created alongside the token,
 * because the gateway still authenticates via session until Step 2. The token is
 * purely additive here and becomes the primary mechanism once the gateway migrates.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    /** Signed HS256 JWT access token. */
    private String token;

    /** Token scheme; always {@code "Bearer"} (use as {@code Authorization: Bearer <token>}). */
    private String type;

    /** Token lifetime in seconds. */
    private long expiresIn;

    /** The authenticated user (never contains the password). */
    private UserResponse user;
}
