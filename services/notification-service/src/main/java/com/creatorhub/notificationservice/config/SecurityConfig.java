package com.creatorhub.notificationservice.config;

import com.creatorhub.common.security.CurrentViewerService;
import com.creatorhub.common.security.HeaderAuthenticationFilter;
import com.creatorhub.common.security.RestAccessDeniedHandler;
import com.creatorhub.common.security.RestAuthenticationEntryPoint;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Stateless security for the Notification service. Identity comes from the
 * gateway-injected headers ({@link HeaderAuthenticationFilter}); no session, no CSRF.
 * Notifications are always per-user, so all /api/notifications endpoints require auth.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    RestAuthenticationEntryPoint authenticationEntryPoint(ObjectMapper objectMapper) {
        return new RestAuthenticationEntryPoint(objectMapper);
    }

    @Bean
    RestAccessDeniedHandler accessDeniedHandler(ObjectMapper objectMapper) {
        return new RestAccessDeniedHandler(objectMapper);
    }

    @Bean
    CurrentViewerService currentViewerService() {
        return new CurrentViewerService();
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http,
                                    RestAuthenticationEntryPoint authenticationEntryPoint,
                                    RestAccessDeniedHandler accessDeniedHandler) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(new HeaderAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**",
                                "/v3/api-docs/**", "/v3/api-docs.yaml", "/error").permitAll()
                        // Internal service-to-service notification creation (not routed by the gateway).
                        .requestMatchers("/internal/**").permitAll()
                        // A user's own notifications — always authenticated.
                        .requestMatchers("/api/notifications/**").authenticated()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll())
                .exceptionHandling(eh -> eh
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler));

        return http.build();
    }
}
