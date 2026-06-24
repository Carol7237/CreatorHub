package com.creatorhub.gateway.config;

import com.creatorhub.security.jwt.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the shared {@link JwtService} so the gateway can VALIDATE the HS256 access
 * tokens that the User Service signs. HS256 is symmetric: the secret here MUST match
 * the User Service's {@code creatorhub.jwt.secret} (same value/env), otherwise valid
 * tokens would be rejected.
 *
 * <p>The gateway never signs tokens — the validity is irrelevant to validation but the
 * {@code JwtService} constructor requires a positive value, so it is read from the same
 * property (default 7200s) purely to keep both sides configured identically.
 */
@Configuration
public class JwtConfig {

    @Bean
    JwtService jwtService(
            @Value("${creatorhub.jwt.secret}") String secret,
            @Value("${creatorhub.jwt.access-token-validity-seconds:7200}") long accessTokenValiditySeconds) {
        return new JwtService(secret, accessTokenValiditySeconds);
    }
}
