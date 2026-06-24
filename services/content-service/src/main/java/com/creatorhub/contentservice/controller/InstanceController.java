package com.creatorhub.contentservice.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Load-balancing demo endpoint. Each running instance gets a distinct
 * {@code instanceId} (generated once at startup) and reports its container
 * hostname/port. Hitting {@code GET /api/content/instance} repeatedly THROUGH THE
 * GATEWAY shows the response alternating between instances — proof that Spring Cloud
 * LoadBalancer (via Eureka, {@code lb://content-service}) distributes the traffic.
 *
 * <p>Public (no auth) so it is trivial to demonstrate with a few curl calls.
 */
@RestController
@RequestMapping("/api/content")
public class InstanceController {

    /** Unique per JVM/instance — the value that changes between instances. */
    private static final String INSTANCE_ID = UUID.randomUUID().toString().substring(0, 8);

    @Value("${server.port}")
    private int port;

    /**
     * Defined ONLY in the Config Server's centralized config-repo. If this shows the
     * "Config Server" value, the property came from there; the default ("LOCAL ...")
     * means the Config Server was not reachable (the import is `optional:`).
     */
    @Value("${creatorhub.config-source:LOCAL (config server not used)}")
    private String configSource;

    @GetMapping("/instance")
    public Map<String, Object> instance() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("service", "content-service");
        body.put("instanceId", INSTANCE_ID);
        body.put("host", hostname());
        body.put("port", port);
        body.put("configSource", configSource);
        return body;
    }

    private static String hostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "unknown";
        }
    }
}
