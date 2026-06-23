package com.creatorhub.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Resolves the caller's identity ONCE at the single ingress (the gateway) and
 * forwards it to downstream services as trusted headers.
 *
 * <ol>
 *   <li><b>Anti-spoofing:</b> any client-supplied {@code X-User-*} header is stripped
 *       FIRST, so a client cannot impersonate anyone by sending these headers.</li>
 *   <li><b>Resolve:</b> if a session cookie is present, call user-service
 *       {@code /api/auth/me} (load-balanced via Eureka) forwarding the cookie.</li>
 *   <li><b>Inject:</b> on success, set {@code X-User-Id} / {@code X-User-Roles}
 *       (ROLE_-prefixed) / {@code X-User-Name}. On failure or no session, the
 *       request proceeds anonymously (public endpoints still work).</li>
 * </ol>
 *
 * <p>Transitional, pre-JWT. At the JWT step this filter validates a JWT instead of
 * calling {@code /me}, injecting the same headers — downstream services stay unchanged.
 */
@Component
public class IdentityPropagationFilter implements GlobalFilter, Ordered {

    // Kept in sync with com.creatorhub.common.security.GatewayHeaders (not imported:
    // the reactive gateway must not depend on the servlet-based common module).
    static final String USER_ID = "X-User-Id";
    static final String USER_ROLES = "X-User-Roles";
    static final String USER_NAME = "X-User-Name";

    private static final Logger log = LoggerFactory.getLogger(IdentityPropagationFilter.class);

    private final WebClient webClient;

    public IdentityPropagationFilter(WebClient.Builder loadBalancedWebClientBuilder) {
        this.webClient = loadBalancedWebClientBuilder.baseUrl("http://user-service").build();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 1. Strip any client-supplied identity headers (trust only what we inject).
        ServerHttpRequest stripped = exchange.getRequest().mutate()
                .headers(h -> {
                    h.remove(USER_ID);
                    h.remove(USER_ROLES);
                    h.remove(USER_NAME);
                })
                .build();

        String cookie = stripped.getHeaders().getFirst(HttpHeaders.COOKIE);
        if (cookie == null || !cookie.contains("JSESSIONID")) {
            // No session -> anonymous. Continue with stripped headers.
            return chain.filter(exchange.mutate().request(stripped).build());
        }

        // 2. Resolve identity from user-service, forwarding the session cookie.
        return webClient.get()
                .uri("/api/auth/me")
                .header(HttpHeaders.COOKIE, cookie)
                .retrieve()
                .bodyToMono(Map.class)
                .flatMap(me -> {
                    Object id = me.get("id");
                    Object role = me.get("role");
                    Object username = me.get("username");
                    ServerHttpRequest authed = stripped.mutate()
                            .headers(h -> {
                                if (id != null) {
                                    h.set(USER_ID, String.valueOf(id));
                                }
                                if (role != null) {
                                    h.set(USER_ROLES, "ROLE_" + role);
                                }
                                if (username != null) {
                                    h.set(USER_NAME, String.valueOf(username));
                                }
                            })
                            .build();
                    return chain.filter(exchange.mutate().request(authed).build());
                })
                // 3. Not authenticated (401) or user-service hiccup -> proceed anonymously.
                .onErrorResume(ex -> {
                    log.debug("Identity resolution skipped ({}). Proceeding anonymously.", ex.toString());
                    return chain.filter(exchange.mutate().request(stripped).build());
                });
    }

    @Override
    public int getOrder() {
        // Run early, before routing, so identity headers are present when forwarded.
        return -1;
    }
}
