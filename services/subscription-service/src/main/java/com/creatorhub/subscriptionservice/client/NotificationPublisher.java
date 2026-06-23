package com.creatorhub.subscriptionservice.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.stereotype.Service;

/**
 * Fires notifications to notification-service, isolated behind a circuit breaker.
 *
 * <p>FALLBACK = FAIL-OPEN: a notification is best-effort, so if notification-service
 * is down/slow the failure is swallowed (logged only) and the caller's main operation
 * (the subscription) still succeeds. This is the OPPOSITE of premium gating (which is
 * fail-closed): a missed notification is harmless, but a subscription blocked by a
 * notification outage would be unacceptable.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationPublisher {

    private static final String CB_NAME = "notifications";

    private final NotificationClient notificationClient;
    private final CircuitBreakerFactory<?, ?> circuitBreakerFactory;

    public void publish(NotifyRequest request) {
        circuitBreakerFactory.create(CB_NAME).run(
                () -> {
                    notificationClient.create(request);
                    return true;
                },
                throwable -> {
                    log.warn("Notification dispatch failed (best-effort, ignored) recipient={} type={}: {}",
                            request.recipientId(), request.type(), throwable.toString());
                    return false;
                });
    }
}
