package com.creatorhub.config;

import com.creatorhub.dto.UserRequest;
import com.creatorhub.model.enums.Role;
import com.creatorhub.repository.UserRepository;
import com.creatorhub.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Seeds a default ADMIN account on the <b>dev</b> profile only (never in test or
 * production). Credentials are development-only and documented in CLAUDE.md /
 * README.md. Idempotent: does nothing if the admin already exists.
 */
@Component
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class DevDataSeeder implements ApplicationRunner {

    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "admin123"; // DEV ONLY

    private final UserRepository userRepository;
    private final UserService userService;

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.existsByUsername(ADMIN_USERNAME)) {
            return;
        }
        userService.create(UserRequest.builder()
                .username(ADMIN_USERNAME)
                .email("admin@creatorhub.local")
                .password(ADMIN_PASSWORD)
                .displayName("Administrator")
                .role(Role.ADMIN)
                .build());
        log.warn("Seeded DEV admin account '{}' (development credentials only).", ADMIN_USERNAME);
    }
}
