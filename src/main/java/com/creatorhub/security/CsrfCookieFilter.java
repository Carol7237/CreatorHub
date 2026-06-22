package com.creatorhub.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Forces the deferred {@link CsrfToken} to be loaded on every request so the
 * {@code XSRF-TOKEN} cookie is actually written to the response. Without this,
 * a fresh client (our static page or the React SPA) would never receive a token
 * to send back. Used together with {@link SpaCsrfTokenRequestHandler}.
 */
public class CsrfCookieFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrfToken != null) {
            // Touch the token to trigger the CookieCsrfTokenRepository to render it.
            csrfToken.getToken();
        }
        filterChain.doFilter(request, response);
    }
}
