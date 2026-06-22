package com.creatorhub.controller;

import com.creatorhub.dto.UserResponse;
import com.creatorhub.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Administrative endpoints. Protected two ways (defense in depth): the URL rule
 * {@code /api/admin/** -> hasRole('ADMIN')} in SecurityConfig, plus method
 * security here. More admin operations arrive in the Views phase.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;

    /** Full list of all users (admin-only). */
    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserResponse> getAllUsers() {
        return userService.findAll();
    }
}
