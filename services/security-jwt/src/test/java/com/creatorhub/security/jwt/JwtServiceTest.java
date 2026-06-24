package com.creatorhub.security.jwt;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for the pure JWT utility: round-trip, tampering, wrong key, expiry and
 * weak-secret rejection. No Spring, no DB — just the signing/validation logic.
 */
class JwtServiceTest {

    // 64-char DEV/test secret (>= 256 bits). Test-only, never a real secret.
    private static final String SECRET = "test-only-creatorhub-jwt-secret-0123456789-abcdefghijklmnopqrst";

    private final JwtService jwt = new JwtService(SECRET, 3600);

    @Test
    @DisplayName("generate then parse: round-trips userId + username + roles")
    void generateThenParse_roundTrip() {
        String token = jwt.generateAccessToken(42L, "alice", List.of("ROLE_USER", "ROLE_ADMIN"));

        // A JWT is three base64url parts (header.payload.signature) separated by dots.
        assertThat(token.split("\\.")).hasSize(3);

        JwtPrincipal principal = jwt.parse(token);
        assertThat(principal.userId()).isEqualTo(42L);
        assertThat(principal.username()).isEqualTo("alice");
        assertThat(principal.roles()).containsExactly("ROLE_USER", "ROLE_ADMIN");
    }

    @Test
    @DisplayName("tampered signature is rejected")
    void parse_tamperedSignature_rejected() {
        String token = jwt.generateAccessToken(1L, "bob", List.of("ROLE_USER"));
        char last = token.charAt(token.length() - 1);
        String tampered = token.substring(0, token.length() - 1) + (last == 'a' ? 'b' : 'a');

        assertThatThrownBy(() -> jwt.parse(tampered)).isInstanceOf(JwtException.class);
        assertThat(jwt.tryParse(tampered)).isEmpty();
    }

    @Test
    @DisplayName("token signed with a different secret is rejected")
    void parse_differentSecret_rejected() {
        String token = jwt.generateAccessToken(1L, "bob", List.of("ROLE_USER"));
        JwtService other = new JwtService("another-totally-different-secret-0123456789-abcdefghijklmnop", 3600);
        assertThat(other.tryParse(token)).isEmpty();
    }

    @Test
    @DisplayName("expired token is rejected")
    void parse_expiredToken_rejected() throws InterruptedException {
        JwtService shortLived = new JwtService(SECRET, 1);
        String token = shortLived.generateAccessToken(1L, "carol", List.of("ROLE_USER"));
        Thread.sleep(1100);
        assertThat(shortLived.tryParse(token)).isEmpty();
    }

    @Test
    @DisplayName("tryParse returns empty for null/blank input")
    void tryParse_nullOrBlank_empty() {
        assertThat(jwt.tryParse(null)).isEmpty();
        assertThat(jwt.tryParse("   ")).isEmpty();
    }

    @Test
    @DisplayName("constructor rejects a weak (too short) secret")
    void constructor_rejectsWeakSecret() {
        assertThatThrownBy(() -> new JwtService("too-short", 3600))
                .isInstanceOf(Exception.class);
    }
}
