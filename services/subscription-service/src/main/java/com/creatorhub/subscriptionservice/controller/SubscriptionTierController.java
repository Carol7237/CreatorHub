package com.creatorhub.subscriptionservice.controller;

import com.creatorhub.common.security.CurrentViewerService;
import com.creatorhub.subscriptionservice.dto.SubscriptionTierRequest;
import com.creatorhub.subscriptionservice.dto.SubscriptionTierResponse;
import com.creatorhub.subscriptionservice.service.SubscriptionTierService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/tiers")
@RequiredArgsConstructor
public class SubscriptionTierController {

    private final SubscriptionTierService tierService;
    private final CurrentViewerService currentViewerService;

    /** Public: list tiers, optionally filtered by creator (replaces /api/creators/{id}/tiers). */
    @GetMapping
    public List<SubscriptionTierResponse> list(@RequestParam(required = false) Long creatorId) {
        return creatorId != null ? tierService.findByCreator(creatorId) : tierService.findAll();
    }

    @GetMapping("/{id}")
    public SubscriptionTierResponse getById(@PathVariable Long id) {
        return tierService.findById(id);
    }

    /** Authenticated: create a tier (creator = current user). */
    @PostMapping
    public ResponseEntity<SubscriptionTierResponse> create(@Valid @RequestBody SubscriptionTierRequest request) {
        SubscriptionTierResponse created = tierService.create(request, currentViewerService.requireViewer());
        return ResponseEntity.created(URI.create("/api/tiers/" + created.getId())).body(created);
    }

    @PutMapping("/{id}")
    public SubscriptionTierResponse update(@PathVariable Long id, @Valid @RequestBody SubscriptionTierRequest request) {
        return tierService.update(id, request, currentViewerService.requireViewer());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        tierService.delete(id, currentViewerService.requireViewer());
        return ResponseEntity.noContent().build();
    }
}
