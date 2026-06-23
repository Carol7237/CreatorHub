package com.creatorhub.contentservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Declarative HTTP client for the Subscription service, resolved through Eureka and
 * load-balanced (target = service-id "subscription-service", no hard-coded URL).
 * Used only for the internal premium-gating contract.
 */
@FeignClient(name = "subscription-service", path = "/internal/subscriptions")
public interface SubscriptionClient {

    @GetMapping("/access")
    SubscriptionAccessResponse getAccess(@RequestParam("fanId") Long fanId,
                                         @RequestParam("tierId") Long tierId);
}
