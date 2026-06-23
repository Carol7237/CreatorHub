package com.creatorhub.common.security;

/**
 * Names of the trusted identity headers the API gateway injects into requests it
 * forwards to downstream services (after authenticating the session and stripping
 * any client-supplied copies). Downstream services read identity ONLY from these.
 *
 * <p>Transitional, pre-JWT: the gateway resolves identity by calling user-service
 * {@code /api/auth/me}. At the JWT step the gateway will validate a JWT instead and
 * inject the same headers, so downstream services stay unchanged.
 */
public final class GatewayHeaders {

    /** Authenticated user's id. */
    public static final String USER_ID = "X-User-Id";

    /** Comma-separated granted authorities, already ROLE_-prefixed (e.g. "ROLE_ADMIN"). */
    public static final String USER_ROLES = "X-User-Roles";

    /** Authenticated username (for logging/diagnostics only). */
    public static final String USER_NAME = "X-User-Name";

    private GatewayHeaders() {
    }
}
