package com.creatorhub.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * API Gateway: the single entry point for all client traffic. Routes are defined
 * declaratively in application.yml and target services by their Eureka service-id
 * through {@code lb://}, so the gateway never hard-codes service host/port.
 */
@SpringBootApplication
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
