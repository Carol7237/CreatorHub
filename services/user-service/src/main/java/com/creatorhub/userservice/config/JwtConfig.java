package com.creatorhub.userservice.config;

import com.creatorhub.security.jwt.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the shared {@link JwtService} (from the security-jwt module) with the HS256
 * secret and token lifetime read from configuration ({@code creatorhub.jwt.*}).
 *
 * <p>The secret has a documented DEV-ONLY default in {@code application.yml} and is
 * meant to be externalized (env {@code CREATORHUB_JWT_SECRET} or the Config Server)
 * for real deployments. The same secret will be configured on the gateway (Step 2)
 * so it can validate the tokens this service signs.
 */
@Configuration
public class JwtConfig {

    @Bean
    JwtService jwtService(
            @Value("${creatorhub.jwt.secret}") String secret,
            @Value("${creatorhub.jwt.access-token-validity-seconds}") long accessTokenValiditySeconds) {
        return new JwtService(secret, accessTokenValiditySeconds);
    }
}
