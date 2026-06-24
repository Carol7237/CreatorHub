package com.creatorhub.userservice.controller;

import com.creatorhub.common.dto.ApiErrorResponse;
import com.creatorhub.security.jwt.JwtService;
import com.creatorhub.userservice.dto.LoginRequest;
import com.creatorhub.userservice.dto.LoginResponse;
import com.creatorhub.userservice.dto.RegisterRequest;
import com.creatorhub.userservice.dto.UserRequest;
import com.creatorhub.userservice.dto.UserResponse;
import com.creatorhub.userservice.model.enums.Role;
import com.creatorhub.userservice.security.CurrentUserService;
import com.creatorhub.userservice.security.SecurityUser;
import com.creatorhub.userservice.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

/**
 * Authentication endpoints (stateless / JWT — Step 2).
 *
 * <ul>
 *   <li>{@code POST /api/auth/register} — public self-registration (always USER).</li>
 *   <li>{@code POST /api/auth/login} — validates credentials (BCrypt, unchanged) and
 *       returns a signed JWT ({@link LoginResponse}). No session, no CSRF.</li>
 *   <li>{@code GET /api/auth/me} — the current user, resolved from the gateway-injected
 *       identity ({@code X-User-Id} header → SecurityContext via HeaderAuthenticationFilter),
 *       consistent with the other downstream services.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final CurrentUserService currentUserService;

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
     * Validates the credentials (BCrypt via {@link AuthenticationManager}, unchanged) and
     * returns a signed JWT in {@link LoginResponse}. Stateless: no session is created and
     * no CSRF token is required (the JWT travels in the Authorization header).
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request,
                                   HttpServletRequest httpRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            request.getUsername(), request.getPassword()));

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
                    .user(userService.findById(principal.getId()))
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

    /** The current user, resolved from the gateway-injected identity (handy for the SPA). */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> me() {
        Long userId = currentUserService.requireViewer().userId();
        return ResponseEntity.ok(userService.findById(userId));
    }
}
