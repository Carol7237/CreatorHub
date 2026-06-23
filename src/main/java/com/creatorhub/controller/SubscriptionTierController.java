package com.creatorhub.controller;

import com.creatorhub.dto.SubscriptionTierRequest;
import com.creatorhub.dto.SubscriptionTierResponse;
import com.creatorhub.security.CurrentUserService;
import com.creatorhub.service.SubscriptionTierService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/tiers")
@RequiredArgsConstructor
public class SubscriptionTierController {

    private final SubscriptionTierService tierService;
    private final CurrentUserService currentUserService;

    @GetMapping("/{id}")
    public SubscriptionTierResponse getById(@PathVariable Long id) {
        return tierService.findById(id);
    }

    /** Authenticated: create a tier (creator = current user). */
    @PostMapping
    public ResponseEntity<SubscriptionTierResponse> create(@Valid @RequestBody SubscriptionTierRequest request) {
        SubscriptionTierResponse created = tierService.create(request, currentUserService.requireViewer());
        return ResponseEntity.created(URI.create("/api/tiers/" + created.getId())).body(created);
    }

    @PutMapping("/{id}")
    public SubscriptionTierResponse update(@PathVariable Long id, @Valid @RequestBody SubscriptionTierRequest request) {
        return tierService.update(id, request, currentUserService.requireViewer());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        tierService.delete(id, currentUserService.requireViewer());
        return ResponseEntity.noContent().build();
    }
}
