package com.creatorhub.userservice.controller;

import com.creatorhub.common.dto.PagedResponse;
import com.creatorhub.userservice.dto.UserResponse;
import com.creatorhub.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public browsing of creators (users). Read-only.
 *
 * <p>Microservices note: the monolith's {@code /api/creators/{id}/posts} and
 * {@code /{id}/tiers} sub-resources are intentionally NOT here — they belong to
 * the Content and Subscription services and will be exposed through the gateway
 * once those services are migrated.
 */
@RestController
@RequestMapping("/api/creators")
@RequiredArgsConstructor
public class CreatorController {

    private final UserService userService;

    @GetMapping
    public PagedResponse<UserResponse> list(Pageable pageable) {
        return userService.findAll(pageable);
    }

    @GetMapping("/{id}")
    public UserResponse getById(@PathVariable Long id) {
        return userService.findById(id);
    }
}
