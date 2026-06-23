package com.creatorhub.security;

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
 * Stateful, session-based security for an API consumed by our static login page
 * and (later) a React SPA. CSRF stays active (cookie-based), passwords are
 * BCrypt-encoded, authorization is by URL pattern + roles, and remember-me is
 * token-based.
 *
 * <p>JWT note: at the microservices stage a JWT filter is added on top of this;
 * it reuses {@link CustomUserDetailsService} and the {@link PasswordEncoder}
 * unchanged. Only the session bits are swapped for stateless JWT there.
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
                        // Static assets + the custom login page.
                        .requestMatchers("/", "/index.html", "/login", "/login.html",
                                "/css/**", "/js/**", "/favicon.ico", "/error").permitAll()
                        // API docs (Swagger UI) — dev convenience.
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/v3/api-docs.yaml").permitAll()
                        // Public auth endpoints (register/login + CSRF token bootstrap).
                        .requestMatchers("/api/auth/register", "/api/auth/login", "/api/auth/csrf").permitAll()
                        // Public reads (browsing creators, public profiles, posts, tags).
                        // Premium-content gating is enforced in the service layer, not by URL.
                        .requestMatchers(HttpMethod.GET,
                                "/api/creators/**", "/api/profiles/**", "/api/posts/**", "/api/tags/**").permitAll()
                        // Admin-only area.
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        // Any other API endpoint requires authentication.
                        .requestMatchers("/api/**").authenticated()
                        // Non-API routes (static, unknown browser routes) are open so that
                        // unknown paths render the custom 404 page (not a 401), and the SPA
                        // can own client-side routing. No sensitive endpoint lives outside /api.
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
