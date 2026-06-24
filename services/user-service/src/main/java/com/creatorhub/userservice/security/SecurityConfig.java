package com.creatorhub.userservice.security;

import com.creatorhub.common.security.HeaderAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Stateless, JWT-era security for the User Service (Step 2). Authentication has two halves:
 *
 * <ul>
 *   <li><b>Login</b> ({@code /api/auth/login}, public) validates username/password with
 *       {@link CustomUserDetailsService} + BCrypt (reused unchanged) and returns a signed
 *       JWT. No session is created and NO CSRF token is required: a JWT travels in the
 *       {@code Authorization} header, which the browser does not send automatically, so it
 *       is not vulnerable to CSRF the way an ambient session cookie is.</li>
 *   <li><b>Protected endpoints</b> ({@code /api/auth/me}, {@code /api/admin/**}) are
 *       authenticated from the gateway-injected identity headers via
 *       {@link HeaderAuthenticationFilter} — exactly like the other downstream services.
 *       The principal is the user's id (a {@code Long}).</li>
 * </ul>
 *
 * <p>This replaces the previous session + CSRF + remember-me setup: the gateway no longer
 * depends on a session (it validates the JWT and injects the trusted {@code X-User-*}
 * headers). Like the downstream services, identity headers are trusted because the gateway
 * is the only ingress and strips any client-supplied copies.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Stateless JWT auth: no session, no CSRF (token is in the Authorization header).
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Identity from the gateway-injected X-User-* headers (same as downstream services).
                .addFilterBefore(new HeaderAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        // CORS preflight.
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // API docs (Swagger UI) + error — dev convenience.
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**",
                                "/v3/api-docs/**", "/v3/api-docs.yaml", "/error").permitAll()
                        // Public auth endpoints (register + login). No CSRF bootstrap needed anymore.
                        .requestMatchers("/api/auth/register", "/api/auth/login").permitAll()
                        // Public reads (browse creators, public profiles).
                        .requestMatchers(HttpMethod.GET, "/api/creators/**", "/api/profiles/**").permitAll()
                        // Admin-only area.
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        // Any other API endpoint requires an authenticated identity.
                        .requestMatchers("/api/**").authenticated()
                        // No sensitive endpoint lives outside /api; leave the rest open.
                        .anyRequest().permitAll())
                .exceptionHandling(eh -> eh
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler));

        return http.build();
    }
}
