package com.creatorhub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Application entry point for CreatorHub.
 *
 * <p>Phase 0 (Setup): this is the bare Spring Boot monolith skeleton. Business
 * packages (controller/service/repository/model/dto/exception/config) are created
 * but intentionally empty — they will be populated in later phases.
 */
@SpringBootApplication
public class CreatorHubApplication {

    public static void main(String[] args) {
        SpringApplication.run(CreatorHubApplication.class, args);
    }
}
