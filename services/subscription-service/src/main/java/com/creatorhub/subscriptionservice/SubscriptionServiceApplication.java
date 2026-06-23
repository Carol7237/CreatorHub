package com.creatorhub.subscriptionservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Subscriptions microservice (SubscriptionTier + Subscription). Component scanning
 * is widened to {@code com.creatorhub} so the shared {@code common} beans (global
 * exception handler) are picked up; entity/repository scanning stays at this package.
 */
@SpringBootApplication(scanBasePackages = "com.creatorhub")
public class SubscriptionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SubscriptionServiceApplication.class, args);
    }
}
