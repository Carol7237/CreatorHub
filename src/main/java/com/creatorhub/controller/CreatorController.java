package com.creatorhub.controller;

import com.creatorhub.dto.PagedResponse;
import com.creatorhub.dto.PostResponse;
import com.creatorhub.dto.SubscriptionTierResponse;
import com.creatorhub.dto.UserResponse;
import com.creatorhub.security.CurrentUserService;
import com.creatorhub.service.PostService;
import com.creatorhub.service.SubscriptionTierService;
import com.creatorhub.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Public browsing of creators (users) and their public content. All read-only.
 */
@RestController
@RequestMapping("/api/creators")
@RequiredArgsConstructor
public class CreatorController {

    private final UserService userService;
    private final PostService postService;
    private final SubscriptionTierService tierService;
    private final CurrentUserService currentUserService;

    @GetMapping
    public PagedResponse<UserResponse> list(Pageable pageable) {
        return userService.findAll(pageable);
    }

    @GetMapping("/{id}")
    public UserResponse getById(@PathVariable Long id) {
        return userService.findById(id);
    }

    /** A creator's posts (premium bodies locked for viewers without access). */
    @GetMapping("/{id}/posts")
    public PagedResponse<PostResponse> posts(@PathVariable Long id, Pageable pageable) {
        return postService.findByCreator(id, pageable, currentUserService.currentViewer());
    }

    /** A creator's subscription tiers. */
    @GetMapping("/{id}/tiers")
    public List<SubscriptionTierResponse> tiers(@PathVariable Long id) {
        return tierService.findByCreator(id);
    }
}
