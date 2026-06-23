package com.creatorhub.subscriptionservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Subscriptions microservice (SubscriptionTier + Subscription). Component scanning
 * is widened to {@code com.creatorhub} so the shared {@code common} beans (global
 * exception handler) are picked up; entity/repository scanning stays at this package.
 * Feign clients are enabled for the best-effort notification call on subscribe.
 */
@SpringBootApplication(scanBasePackages = "com.creatorhub")
@EnableFeignClients
public class SubscriptionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SubscriptionServiceApplication.class, args);
    }
}
