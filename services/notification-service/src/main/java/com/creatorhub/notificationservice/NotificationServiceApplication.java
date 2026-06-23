package com.creatorhub.notificationservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Notifications microservice, backed by MongoDB (NoSQL). Component scanning is
 * widened to {@code com.creatorhub} so the shared {@code common} beans (global
 * exception handler) are picked up; the Mongo document/repository scanning stays
 * rooted at this package.
 */
@SpringBootApplication(scanBasePackages = "com.creatorhub")
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
