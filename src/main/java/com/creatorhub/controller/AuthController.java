package com.creatorhub.controller;

import com.creatorhub.dto.ApiErrorResponse;
import com.creatorhub.dto.LoginRequest;
import com.creatorhub.dto.RegisterRequest;
import com.creatorhub.dto.UserRequest;
import com.creatorhub.dto.UserResponse;
import com.creatorhub.model.enums.Role;
import com.creatorhub.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * Authentication endpoints. Authentication is session-based but driven via REST
 * (so the static login page and the future React SPA both consume the same
 * endpoints). Logout is handled by Spring Security's LogoutFilter at
 * POST /api/auth/logout.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final RememberMeServices rememberMeServices;

    /** Self-registration. Always creates a USER (never ADMIN). */
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@RequestBody RegisterRequest request) {
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

    /** Programmatic login that establishes an HTTP session (and optional remember-me cookie). */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request,
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

            return ResponseEntity.ok(userService.findByUsername(authentication.getName()));
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
