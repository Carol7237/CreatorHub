package com.creatorhub.subscriptionservice.controller;

import com.creatorhub.subscriptionservice.dto.SubscriptionAccessResponse;
import com.creatorhub.subscriptionservice.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Internal, service-to-service contract for premium gating. The Content service
 * calls this (via Eureka load-balanced Feign) to learn whether a fan holds an
 * ACTIVE subscription to a tier.
 *
 * <p>Lives under {@code /internal/**}, which the API gateway does NOT route, so it
 * is only reachable from inside the cluster (not by external clients).
 */
@RestController
@RequestMapping("/internal/subscriptions")
@RequiredArgsConstructor
public class InternalSubscriptionController {

    private final SubscriptionService subscriptionService;

    @GetMapping("/access")
    public SubscriptionAccessResponse access(@RequestParam Long fanId, @RequestParam Long tierId) {
        boolean active = subscriptionService.hasActiveAccess(fanId, tierId);
        return new SubscriptionAccessResponse(fanId, tierId, active);
    }
}
