package com.creatorhub.userservice.controller;

import com.creatorhub.common.dto.ApiErrorResponse;
import com.creatorhub.security.jwt.JwtService;
import com.creatorhub.userservice.dto.LoginRequest;
import com.creatorhub.userservice.dto.LoginResponse;
import com.creatorhub.userservice.dto.RegisterRequest;
import com.creatorhub.userservice.dto.UserRequest;
import com.creatorhub.userservice.dto.UserResponse;
import com.creatorhub.userservice.model.enums.Role;
import com.creatorhub.userservice.security.SecurityUser;
import com.creatorhub.userservice.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

/**
 * Authentication endpoints. Authentication is session-based but driven via REST
 * (so the React SPA consumes the same endpoints). Logout is handled by Spring
 * Security's LogoutFilter at POST /api/auth/logout.
 *
 * <p>JWT (Step 1, transitional): {@code /api/auth/login} ALSO issues a signed HS256
 * access token in the response body (alongside creating the session). The session
 * is still created because the gateway authenticates via session until Step 2; the
 * token is additive for now. {@code /api/auth/me} stays session-based here.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final RememberMeServices rememberMeServices;
    private final JwtService jwtService;

    /**
     * Returns the CSRF token so a cross-origin SPA can read it from the response
     * body (instead of the cookie) and echo it in the X-XSRF-TOKEN header.
     * The CsrfCookieFilter also sets the XSRF-TOKEN cookie on this request.
     */
    @GetMapping("/csrf")
    public CsrfToken csrf(CsrfToken token) {
        return token;
    }

    /** Self-registration. Always creates a USER (never ADMIN). */
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserRequest userRequest = UserRequest.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(request.getPassword())
                .displayName(request.getDisplayName())
                .role(Role.USER)
                .build();
        UserResponse created = userService.create(userRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Programmatic login. Authenticates the credentials (BCrypt, unchanged), then:
     * (1) establishes an HTTP session (+ optional remember-me cookie) — still used by
     * the gateway until Step 2 — and (2) issues a signed JWT access token returned in
     * the body. Returns {@link LoginResponse} (token + Bearer type + expiresIn + user).
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request,
                                   HttpServletRequest httpRequest,
                                   HttpServletResponse httpResponse) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            request.getUsername(), request.getPassword()));

            // Persist the authentication into the session so later requests are authenticated.
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            securityContextRepository.saveContext(context, httpRequest, httpResponse);

            // Issue the remember-me cookie only when requested.
            HttpServletRequest rememberMeRequest = request.isRememberMe()
                    ? forceRememberMeParam(httpRequest)
                    : httpRequest;
            rememberMeServices.loginSuccess(rememberMeRequest, httpResponse, authentication);

            // Sign a JWT access token for the authenticated principal (additive in Step 1).
            SecurityUser principal = (SecurityUser) authentication.getPrincipal();
            List<String> roles = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .toList();
            String token = jwtService.generateAccessToken(
                    principal.getId(), principal.getUsername(), roles);

            return ResponseEntity.ok(LoginResponse.builder()
                    .token(token)
                    .type("Bearer")
                    .expiresIn(jwtService.getAccessTokenValiditySeconds())
                    .user(userService.findByUsername(authentication.getName()))
                    .build());
        } catch (AuthenticationException ex) {
            ApiErrorResponse error = ApiErrorResponse.builder()
                    .timestamp(Instant.now())
                    .status(HttpStatus.UNAUTHORIZED.value())
                    .error("Unauthorized")
                    .message("Invalid username or password")
                    .path(httpRequest.getRequestURI())
                    .build();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }

    /** The currently authenticated user (handy for the SPA on page load). */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(Authentication authentication) {
        return ResponseEntity.ok(userService.findByUsername(authentication.getName()));
    }

    /** Makes TokenBasedRememberMeServices see the remember-me parameter (sent in the JSON body). */
    private static HttpServletRequest forceRememberMeParam(HttpServletRequest request) {
        return new HttpServletRequestWrapper(request) {
            @Override
            public String getParameter(String name) {
                if ("remember-me".equals(name)) {
                    return "true";
                }
                return super.getParameter(name);
            }
        };
    }
}
