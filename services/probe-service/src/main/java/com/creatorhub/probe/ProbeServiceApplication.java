package com.creatorhub.probe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Throwaway probe service. Its only job in Phase 8 Step 1 is to register with
 * Eureka and expose one endpoint, so we can prove that a request routed through
 * the gateway is discovered and reaches a real backend. It will be removed once
 * the real services exist.
 */
@SpringBootApplication
public class ProbeServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProbeServiceApplication.class, args);
    }
}
