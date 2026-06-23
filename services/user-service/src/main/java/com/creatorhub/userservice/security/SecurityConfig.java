package com.creatorhub.userservice.security;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Session-based security for the User Service, migrated unchanged in spirit from
 * the monolith: CSRF stays active (cookie-based), passwords are BCrypt-encoded,
 * authorization is by URL pattern + roles, remember-me is token-based.
 *
 * <p>Requests normally arrive through the API gateway; the gateway transparently
 * relays the session + XSRF cookies, so the same CSRF flow works end-to-end.
 *
 * <p>JWT note: at a later step a JWT filter is added on top of this, reusing
 * {@link CustomUserDetailsService} and the {@link PasswordEncoder} unchanged; only
 * the session bits become stateless.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    /** Dev/test key for signing remember-me tokens; externalize a real secret for production. */
    static final String REMEMBER_ME_KEY = "creatorhub-remember-me-key-CHANGE-IN-PROD";

    /** Dev front-end origins (Vite / CRA). Restrict to the real domain in production. */
    private static final List<String> ALLOWED_ORIGINS = List.of("http://localhost:5173", "http://localhost:3000");

    private final CustomUserDetailsService userDetailsService;
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
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    RememberMeServices rememberMeServices(UserDetailsService uds) {
        TokenBasedRememberMeServices services = new TokenBasedRememberMeServices(REMEMBER_ME_KEY, uds);
        services.setParameter("remember-me");
        services.setTokenValiditySeconds(14 * 24 * 60 * 60); // 14 days
        return services;
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(ALLOWED_ORIGINS);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        // Allow cookies (JSESSIONID + XSRF-TOKEN) on cross-origin requests for the SPA.
        config.setAllowCredentials(true);
        config.setExposedHeaders(List.of("Location"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http,
                                    RememberMeServices rememberMeServices,
                                    SecurityContextRepository securityContextRepository,
                                    CorsConfigurationSource corsConfigurationSource) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                // Stateful session auth (no httpBasic / no Spring formLogin: login is a REST endpoint).
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler()))
                .addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        // CORS preflight.
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // API docs (Swagger UI) + error — dev convenience.
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**",
                                "/v3/api-docs/**", "/v3/api-docs.yaml", "/error").permitAll()
                        // Public auth endpoints (register/login + CSRF token bootstrap).
                        .requestMatchers("/api/auth/register", "/api/auth/login", "/api/auth/csrf").permitAll()
                        // Public reads (browsing creators, public profiles). Premium-content
                        // gating is enforced in the Content service, not by URL here.
                        .requestMatchers(HttpMethod.GET, "/api/creators/**", "/api/profiles/**").permitAll()
                        // Admin-only area.
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        // Any other API endpoint requires authentication.
                        .requestMatchers("/api/**").authenticated()
                        // No sensitive endpoint lives outside /api; leave the rest open.
                        .anyRequest().permitAll())
                .securityContext(sc -> sc.securityContextRepository(securityContextRepository))
                .rememberMe(rm -> rm
                        .key(REMEMBER_ME_KEY)
                        .rememberMeServices(rememberMeServices))
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .logoutSuccessHandler((req, res, auth) -> res.setStatus(HttpServletResponse.SC_OK))
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID", "remember-me"))
                .exceptionHandling(eh -> eh
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler));

        return http.build();
    }
}
