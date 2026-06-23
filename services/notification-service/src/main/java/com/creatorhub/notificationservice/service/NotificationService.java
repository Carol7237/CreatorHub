package com.creatorhub.notificationservice.service;

import com.creatorhub.common.Viewer;
import com.creatorhub.common.dto.PagedResponse;
import com.creatorhub.notificationservice.dto.CreateNotificationRequest;
import com.creatorhub.notificationservice.dto.NotificationResponse;
import org.springframework.data.domain.Pageable;

public interface NotificationService {

    /** Internal: create a notification (called by other services on an event). */
    NotificationResponse create(CreateNotificationRequest request);

    /** The current user's notifications, newest first. */
    PagedResponse<NotificationResponse> findForViewer(Viewer viewer, Pageable pageable);

    long unreadCount(Viewer viewer);

    /** Mark one notification as read (must belong to the viewer, unless admin). */
    NotificationResponse markRead(String id, Viewer viewer);

    /** Mark all of the viewer's unread notifications as read; returns how many were updated. */
    long markAllRead(Viewer viewer);
}
