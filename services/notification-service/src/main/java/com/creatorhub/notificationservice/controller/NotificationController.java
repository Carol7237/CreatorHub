package com.creatorhub.notificationservice.controller;

import com.creatorhub.common.dto.PagedResponse;
import com.creatorhub.common.security.CurrentViewerService;
import com.creatorhub.notificationservice.dto.NotificationResponse;
import com.creatorhub.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * A user's own notifications. Identity is the gateway-injected current user; a user
 * only ever sees/reads their own notifications (enforced in the service).
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final CurrentViewerService currentViewerService;

    /** The current user's notifications, newest first (paged). */
    @GetMapping
    public PagedResponse<NotificationResponse> list(Pageable pageable) {
        return notificationService.findForViewer(currentViewerService.requireViewer(), pageable);
    }

    /** How many unread notifications the current user has. */
    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount() {
        return Map.of("unread", notificationService.unreadCount(currentViewerService.requireViewer()));
    }

    /** Mark one notification as read. */
    @PostMapping("/{id}/read")
    public NotificationResponse markRead(@PathVariable String id) {
        return notificationService.markRead(id, currentViewerService.requireViewer());
    }

    /** Mark all of the current user's notifications as read. */
    @PostMapping("/read-all")
    public Map<String, Long> markAllRead() {
        return Map.of("updated", notificationService.markAllRead(currentViewerService.requireViewer()));
    }
}
