package com.creatorhub.userservice.controller;

import com.creatorhub.userservice.dto.ProfileRequest;
import com.creatorhub.userservice.dto.ProfileResponse;
import com.creatorhub.userservice.security.CurrentUserService;
import com.creatorhub.userservice.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profiles")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;
    private final CurrentUserService currentUserService;

    /** Public: a profile by its id. */
    @GetMapping("/{id}")
    public ProfileResponse getById(@PathVariable Long id) {
        return profileService.findById(id);
    }

    /** Public: a profile by user id. */
    @GetMapping("/user/{userId}")
    public ProfileResponse getByUserId(@PathVariable Long userId) {
        return profileService.findByUserId(userId);
    }

    /** Authenticated (owner/admin): update a profile. */
    @PutMapping("/{id}")
    public ProfileResponse update(@PathVariable Long id, @Valid @RequestBody ProfileRequest request) {
        return profileService.update(id, request, currentUserService.requireViewer());
    }
}
