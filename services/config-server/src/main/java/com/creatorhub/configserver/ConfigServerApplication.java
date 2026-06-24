package com.creatorhub.configserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

/**
 * Spring Cloud Config Server. Serves centralized configuration to the services from
 * a NATIVE backend (the {@code config-repo/} folder bundled on the classpath). Clients
 * fetch their config at startup via {@code spring.config.import=optional:configserver:...}.
 */
@EnableConfigServer
@SpringBootApplication
public class ConfigServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }
}
