package com.creatorhub.contentservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Content microservice (Post + Comment + Tag). Premium gating calls the
 * Subscription service via an OpenFeign client (load-balanced through Eureka),
 * protected by a Resilience4j circuit breaker. Component scanning is widened to
 * {@code com.creatorhub} so the shared {@code common} beans are picked up;
 * entity/repository scanning stays at this package.
 */
@SpringBootApplication(scanBasePackages = "com.creatorhub")
@EnableFeignClients
public class ContentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ContentServiceApplication.class, args);
    }
}
