package com.creatorhub.userservice.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.util.StringUtils;

import java.util.function.Supplier;

/**
 * CSRF request handler that works for BOTH clients in this project:
 * <ul>
 *   <li>a single-page app sending the raw token in the {@code X-XSRF-TOKEN}
 *       header (cookie-based flow), and</li>
 *   <li>a classic HTML form sending the BREACH-protected (XOR) token as the
 *       {@code _csrf} request parameter.</li>
 * </ul>
 * This is the pattern recommended by the Spring Security reference for SPAs, so
 * there is no conflict between the two CSRF modes.
 */
public final class SpaCsrfTokenRequestHandler implements CsrfTokenRequestHandler {

    private final CsrfTokenRequestHandler plain = new CsrfTokenRequestAttributeHandler();
    private final CsrfTokenRequestHandler xor = new XorCsrfTokenRequestAttributeHandler();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, Supplier<CsrfToken> csrfToken) {
        // Render the token via the XOR handler so a server-rendered form gets a
        // BREACH-protected value; the raw value still lives in the cookie.
        this.xor.handle(request, response, csrfToken);
    }

    @Override
    public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
        // If a header is present (SPA / fetch), the value is the raw token.
        // Otherwise (form post) it is the XOR-encoded parameter value.
        if (StringUtils.hasText(request.getHeader(csrfToken.getHeaderName()))) {
            return this.plain.resolveCsrfTokenValue(request, csrfToken);
        }
        return this.xor.resolveCsrfTokenValue(request, csrfToken);
    }
}
