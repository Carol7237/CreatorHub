package com.creatorhub.contentservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Declarative client for notification-service's internal endpoint (resolved through
 * Eureka, load-balanced). Used only to fire best-effort notifications.
 */
@FeignClient(name = "notification-service", path = "/internal/notifications")
public interface NotificationClient {

    @PostMapping
    void create(@RequestBody NotifyRequest request);
}
