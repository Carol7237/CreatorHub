package com.creatorhub.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Populates the {@link org.springframework.security.core.context.SecurityContext}
 * from the trusted identity headers injected by the gateway ({@link GatewayHeaders}).
 *
 * <p>Used by downstream (stateless) services. The principal is the user's id
 * (a {@code Long}); authorities come from {@code X-User-Roles} (already
 * ROLE_-prefixed). If no id header is present the request stays anonymous, so
 * public endpoints keep working without authentication.
 *
 * <p>SECURITY: this trusts the headers because the gateway is the only ingress and
 * strips any client-supplied copies before injecting the real values. At the JWT
 * step this filter is replaced by token validation.
 */
public class HeaderAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String userId = request.getHeader(GatewayHeaders.USER_ID);
        if (userId != null && !userId.isBlank()
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                Long id = Long.valueOf(userId.trim());
                String roles = request.getHeader(GatewayHeaders.USER_ROLES);
                var authorities = AuthorityUtils.commaSeparatedStringToAuthorityList(
                        roles != null ? roles : "");
                var authentication = new UsernamePasswordAuthenticationToken(id, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (NumberFormatException ex) {
                // Malformed id header -> treat as anonymous (do not authenticate).
                logger.warn("Ignoring malformed " + GatewayHeaders.USER_ID + " header");
            }
        }
        filterChain.doFilter(request, response);
    }
}
