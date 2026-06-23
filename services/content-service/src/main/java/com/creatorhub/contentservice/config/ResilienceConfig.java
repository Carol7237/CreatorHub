package com.creatorhub.contentservice.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Default Resilience4j circuit-breaker configuration for inter-service calls
 * (the premium-gating call Content -> Subscription).
 *
 * <ul>
 *   <li><b>Timeout</b> 3s — a slow Subscription service trips the breaker instead of
 *       hanging the Content request.</li>
 *   <li><b>Sliding window</b> of 10 calls, opens at a 50% failure rate (after at least
 *       5 calls), then stays open 10s before probing again (half-open).</li>
 * </ul>
 * When open or on any failure, {@code SubscriptionAccessService} falls back to
 * fail-closed (no access), so the Content service degrades gracefully.
 */
@Configuration
public class ResilienceConfig {

    @Bean
    public Customizer<Resilience4JCircuitBreakerFactory> defaultCircuitBreakerCustomizer() {
        return factory -> factory.configureDefault(id -> new Resilience4JConfigBuilder(id)
                .timeLimiterConfig(TimeLimiterConfig.custom()
                        .timeoutDuration(Duration.ofSeconds(3))
                        .build())
                .circuitBreakerConfig(CircuitBreakerConfig.custom()
                        .slidingWindowSize(10)
                        .minimumNumberOfCalls(5)
                        .failureRateThreshold(50f)
                        .waitDurationInOpenState(Duration.ofSeconds(10))
                        .permittedNumberOfCallsInHalfOpenState(3)
                        .build())
                .build());
    }
}
