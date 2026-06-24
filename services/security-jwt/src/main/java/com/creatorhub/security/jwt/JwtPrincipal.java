package com.creatorhub.security.jwt;

import java.util.List;

/**
 * Identity extracted from a validated JWT. {@code roles} carry the {@code ROLE_}
 * prefix so they map straight onto Spring Security authorities (hasRole(...)).
 */
public record JwtPrincipal(Long userId, String username, List<String> roles) {
}
