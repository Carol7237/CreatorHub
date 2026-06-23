package com.creatorhub.userservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Identity / user microservice. Component scanning is widened to {@code com.creatorhub}
 * so the shared {@code com.creatorhub.common.web.GlobalExceptionHandler} is picked up.
 * Entity/repository scanning stays rooted at this package (auto-configuration package),
 * so only this service's {@code model}/{@code repository} are mapped.
 */
@SpringBootApplication(scanBasePackages = "com.creatorhub")
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
