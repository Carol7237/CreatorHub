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
    SecurityFilterChain filterChain(HttpSecurity http,
                                    RememberMeServices rememberMeServices,
                                    SecurityContextRepository securityContextRepository) throws Exception {
        http
                // Stateful session auth (no httpBasic / no Spring formLogin: login is a REST endpoint).
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler()))
                .addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        // Static assets + the custom login page.
                        .requestMatchers("/", "/index.html", "/login", "/login.html",
                                "/css/**", "/js/**", "/favicon.ico", "/error").permitAll()
                        // Public auth endpoints.
                        .requestMatchers("/api/auth/register", "/api/auth/login").permitAll()
                        // Public reads (browsing creators, public profiles, posts, tags).
                        // Premium-content gating is enforced in the service layer, not by URL.
                        .requestMatchers(HttpMethod.GET,
                                "/api/creators/**", "/api/profiles/**", "/api/posts/**", "/api/tags/**").permitAll()
                        // Admin-only area.
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        // Everything else requires authentication.
                        .anyRequest().authenticated())
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
