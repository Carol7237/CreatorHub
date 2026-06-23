package com.creatorhub.gateway.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * A load-balanced {@link WebClient.Builder} so the identity filter can call
 * {@code http://user-service/...} (resolved through Eureka + Spring Cloud LoadBalancer)
 * without a hard-coded host/port.
 */
@Configuration
public class WebClientConfig {

    @Bean
    @LoadBalanced
    WebClient.Builder loadBalancedWebClientBuilder() {
        return WebClient.builder();
    }
}
