package com.creatorhub.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Stateless HS256 JWT signing + validation. Plain POJO (no Spring, no web stack) so
 * it can be reused by both the servlet-based User Service and the reactive API
 * Gateway. The secret is symmetric: the User Service signs, the gateway validates
 * with the SAME key (configured identically on both sides).
 *
 * <p>Token shape: {@code sub} = userId, plus a {@code username} claim and a
 * {@code roles} claim (list of {@code ROLE_*}), with standard {@code iat}/{@code exp}.
 */
public class JwtService {

    public static final String CLAIM_USERNAME = "username";
    public static final String CLAIM_ROLES = "roles";

    private final SecretKey key;
    private final long accessTokenValiditySeconds;

    /**
     * @param secret                     HS256 signing secret; must be at least 256 bits
     *                                   (32+ bytes) — a shorter secret fails fast here.
     * @param accessTokenValiditySeconds token lifetime in seconds (must be positive).
     */
    public JwtService(String secret, long accessTokenValiditySeconds) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("JWT secret must not be blank");
        }
        if (accessTokenValiditySeconds <= 0) {
            throw new IllegalArgumentException("JWT validity (seconds) must be positive");
        }
        // hmacShaKeyFor throws WeakKeyException if the secret is < 256 bits -> fail at startup.
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenValiditySeconds = accessTokenValiditySeconds;
    }

    /** Token lifetime in seconds (handy for the {@code expiresIn} field of a login response). */
    public long getAccessTokenValiditySeconds() {
        return accessTokenValiditySeconds;
    }

    /** Signs a new HS256 access token for the given user. */
    public String generateAccessToken(Long userId, String username, Collection<String> roles) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(accessTokenValiditySeconds);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(CLAIM_USERNAME, username)
                .claim(CLAIM_ROLES, List.copyOf(roles))
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Verifies signature + expiry and extracts the identity.
     *
     * @throws JwtException if the token is malformed, tampered, expired or signed
     *                      with a different key.
     */
    public JwtPrincipal parse(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        Long userId = Long.valueOf(claims.getSubject());
        String username = claims.get(CLAIM_USERNAME, String.class);
        List<?> rawRoles = claims.get(CLAIM_ROLES, List.class);
        List<String> roles = rawRoles == null
                ? List.of()
                : rawRoles.stream().map(String::valueOf).toList();
        return new JwtPrincipal(userId, username, roles);
    }

    /**
     * Null/invalid-safe variant: returns empty when the token is missing or fails
     * validation. Convenient for the gateway filter, where an invalid token simply
     * means "anonymous request" rather than an exception.
     */
    public Optional<JwtPrincipal> tryParse(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(parse(token));
        } catch (JwtException | IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
