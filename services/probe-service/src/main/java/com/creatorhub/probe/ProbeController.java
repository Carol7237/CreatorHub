package com.creatorhub.probe;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Single validation endpoint. The {@code port} in the response makes it obvious,
 * when called through the gateway, which discovered instance actually served the
 * request (useful later to demonstrate load balancing).
 */
@RestController
@RequestMapping("/api/probe")
public class ProbeController {

    @Value("${server.port}")
    private int port;

    @GetMapping("/hello")
    public Map<String, Object> hello() {
        return Map.of(
                "message", "hello from probe-service",
                "service", "probe-service",
                "port", port
        );
    }
}
