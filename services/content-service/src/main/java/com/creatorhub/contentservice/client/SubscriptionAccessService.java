package com.creatorhub.contentservice.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.stereotype.Service;

/**
 * Premium-gating check, isolated behind a Resilience4j circuit breaker.
 *
 * <p>FALLBACK = FAIL-CLOSED: if the Subscription service is down/slow (call fails
 * or the breaker is open), we return {@code false} ("no access"), so a premium post
 * stays locked rather than being wrongly revealed. Security-safe default: when in
 * doubt, deny. The circuit breaker keeps a Subscription outage from cascading into
 * the Content service (no 500s, Content stays up).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionAccessService {

    /** Circuit breaker instance id (configured in ResilienceConfig / application.yml). */
    private static final String CB_NAME = "subscriptionAccess";

    private final SubscriptionClient subscriptionClient;
    private final CircuitBreakerFactory<?, ?> circuitBreakerFactory;

    /** True iff the fan holds an ACTIVE subscription to the tier; false on any failure (fail-closed). */
    public boolean hasActiveSubscription(Long fanId, Long tierId) {
        if (fanId == null || tierId == null) {
            return false;
        }
        return circuitBreakerFactory.create(CB_NAME).run(
                () -> subscriptionClient.getAccess(fanId, tierId).active(),
                throwable -> {
                    log.warn("Premium gating check unavailable (fail-closed -> locked) for fan={} tier={}: {}",
                            fanId, tierId, throwable.toString());
                    return false;
                });
    }
}
