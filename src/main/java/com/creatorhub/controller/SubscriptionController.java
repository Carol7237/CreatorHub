package com.creatorhub.controller;

import com.creatorhub.common.Viewer;
import com.creatorhub.dto.SubscriptionRequest;
import com.creatorhub.dto.SubscriptionResponse;
import com.creatorhub.security.CurrentUserService;
import com.creatorhub.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final CurrentUserService currentUserService;

    /** The current user's own subscriptions. */
    @GetMapping
    public List<SubscriptionResponse> mySubscriptions() {
        Viewer viewer = currentUserService.requireViewer();
        return subscriptionService.findByFan(viewer.userId());
    }

    /** Authenticated: subscribe (fan = current user). */
    @PostMapping
    public ResponseEntity<SubscriptionResponse> create(@Valid @RequestBody SubscriptionRequest request) {
        SubscriptionResponse created = subscriptionService.create(request, currentUserService.requireViewer());
        return ResponseEntity.created(URI.create("/api/subscriptions/" + created.getId())).body(created);
    }

    /** Authenticated (owner/admin): cancel a subscription. */
    @PostMapping("/{id}/cancel")
    public SubscriptionResponse cancel(@PathVariable Long id) {
        return subscriptionService.cancel(id, currentUserService.requireViewer());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        subscriptionService.delete(id, currentUserService.requireViewer());
        return ResponseEntity.noContent().build();
    }
}
