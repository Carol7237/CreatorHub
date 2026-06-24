package com.creatorhub.gateway.filter;

import com.creatorhub.security.jwt.JwtPrincipal;
import com.creatorhub.security.jwt.JwtService;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Optional;

/**
 * Resolves the caller's identity ONCE at the single ingress (the gateway) from a JWT
 * and forwards it to downstream services as trusted headers.
 *
 * <ol>
 *   <li><b>Anti-spoofing:</b> any client-supplied {@code X-User-*} header is stripped
 *       FIRST, so a client cannot impersonate anyone by sending these headers — the
 *       gateway remains the only source of truth for identity.</li>
 *   <li><b>Validate:</b> read {@code Authorization: Bearer <jwt>} and validate the
 *       signature + expiry (HS256, shared secret) with {@link JwtService}.</li>
 *   <li><b>Inject:</b> on a valid token, set {@code X-User-Id} / {@code X-User-Roles}
 *       (comma-separated, ROLE_-prefixed) / {@code X-User-Name}. Missing/invalid/expired
 *       token -> no identity injected, the request proceeds anonymously (public
 *       endpoints still work; protected ones get 401 downstream).</li>
 * </ol>
 *
 * <p>Validation is CPU-bound and synchronous — fine inside this reactive filter (no
 * blocking I/O, unlike the previous session lookup that called user-service {@code /me}).
 * The downstream services are unchanged: only the SOURCE of identity moved from session
 * to JWT.
 */
@Component
public class IdentityPropagationFilter implements GlobalFilter, Ordered {

    // Kept in sync with com.creatorhub.common.security.GatewayHeaders (not imported:
    // the reactive gateway must not depend on the servlet-based common module).
    static final String USER_ID = "X-User-Id";
    static final String USER_ROLES = "X-User-Roles";
    static final String USER_NAME = "X-User-Name";

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    public IdentityPropagationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        Optional<JwtPrincipal> principal = bearerToken(exchange.getRequest())
                .flatMap(jwtService::tryParse);

        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(h -> {
                    // 1. Strip any client-supplied identity headers (trust only what we inject).
                    h.remove(USER_ID);
                    h.remove(USER_ROLES);
                    h.remove(USER_NAME);
                    // 2. Inject identity ONLY from a validated token.
                    principal.ifPresent(p -> {
                        h.set(USER_ID, String.valueOf(p.userId()));
                        h.set(USER_ROLES, String.join(",", p.roles()));
                        if (p.username() != null) {
                            h.set(USER_NAME, p.username());
                        }
                    });
                })
                .build();

        return chain.filter(exchange.mutate().request(request).build());
    }

    private static Optional<String> bearerToken(ServerHttpRequest request) {
        String header = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            return Optional.of(header.substring(BEARER_PREFIX.length()).trim());
        }
        return Optional.empty();
    }

    @Override
    public int getOrder() {
        // Run early, before routing, so identity headers are present when forwarded.
        return -1;
    }
}
