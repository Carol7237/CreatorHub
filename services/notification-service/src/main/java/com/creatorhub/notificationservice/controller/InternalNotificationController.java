package com.creatorhub.notificationservice.controller;

import com.creatorhub.notificationservice.dto.CreateNotificationRequest;
import com.creatorhub.notificationservice.dto.NotificationResponse;
import com.creatorhub.notificationservice.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Internal, service-to-service endpoint: other services (subscription/content) call
 * this on an event to create a notification. Lives under {@code /internal/**}, which
 * the gateway does NOT route, so it is reachable only from inside the cluster.
 */
@RestController
@RequestMapping("/internal/notifications")
@RequiredArgsConstructor
public class InternalNotificationController {

    private final NotificationService notificationService;

    @PostMapping
    public ResponseEntity<NotificationResponse> create(@Valid @RequestBody CreateNotificationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(notificationService.create(request));
    }
}
