package com.creatorhub.notificationservice.service.impl;

import com.creatorhub.common.Viewer;
import com.creatorhub.common.exception.ResourceNotFoundException;
import com.creatorhub.notificationservice.dto.CreateNotificationRequest;
import com.creatorhub.notificationservice.dto.NotificationResponse;
import com.creatorhub.notificationservice.model.Notification;
import com.creatorhub.notificationservice.model.NotificationType;
import com.creatorhub.notificationservice.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock private NotificationRepository notificationRepository;
    @InjectMocks private NotificationServiceImpl service;

    private static Notification notif(String id, Long recipient, boolean read) {
        Notification n = new Notification();
        n.setId(id);
        n.setRecipientId(recipient);
        n.setType(NotificationType.NEW_SUBSCRIBER);
        n.setMessage("hi");
        n.setRead(read);
        return n;
    }

    @Test
    void create_persistsNotification() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
            Notification n = inv.getArgument(0);
            n.setId("abc");
            return n;
        });
        CreateNotificationRequest req = CreateNotificationRequest.builder()
                .recipientId(7L).type(NotificationType.NEW_COMMENT).message("new comment").actorId(2L).relatedId(5L).build();

        NotificationResponse resp = service.create(req);

        assertThat(resp.getId()).isEqualTo("abc");
        assertThat(resp.getRecipientId()).isEqualTo(7L);
        assertThat(resp.getType()).isEqualTo(NotificationType.NEW_COMMENT);
        assertThat(resp.isRead()).isFalse();
    }

    @Test
    void findForViewer_defaultsToNewestFirst_andMaps() {
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        when(notificationRepository.findByRecipientId(eq(7L), captor.capture()))
                .thenReturn(new PageImpl<>(List.of(notif("a", 7L, false))));

        var page = service.findForViewer(new Viewer(7L, false), PageRequest.of(0, 20));

        assertThat(page.getContent()).hasSize(1);
        // service applied a default createdAt DESC sort to the unsorted request
        assertThat(captor.getValue().getSort().getOrderFor("createdAt")).isNotNull();
        assertThat(captor.getValue().getSort().getOrderFor("createdAt").isDescending()).isTrue();
    }

    @Test
    void unreadCount_delegatesToRepo() {
        when(notificationRepository.countByRecipientIdAndReadFalse(7L)).thenReturn(3L);
        assertThat(service.unreadCount(new Viewer(7L, false))).isEqualTo(3L);
    }

    @Test
    void markRead_byOwner_setsRead() {
        when(notificationRepository.findById("a")).thenReturn(Optional.of(notif("a", 7L, false)));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));
        NotificationResponse resp = service.markRead("a", new Viewer(7L, false));
        assertThat(resp.isRead()).isTrue();
    }

    @Test
    void markRead_byNonOwner_throwsAccessDenied() {
        when(notificationRepository.findById("a")).thenReturn(Optional.of(notif("a", 7L, false)));
        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> service.markRead("a", new Viewer(999L, false)));
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void markRead_notFound_throws() {
        when(notificationRepository.findById("missing")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.markRead("missing", new Viewer(7L, false)));
    }

    @Test
    void markAllRead_marksUnreadAndReturnsCount() {
        when(notificationRepository.findByRecipientIdAndReadFalse(7L))
                .thenReturn(List.of(notif("a", 7L, false), notif("b", 7L, false)));
        long updated = service.markAllRead(new Viewer(7L, false));
        assertThat(updated).isEqualTo(2L);
        verify(notificationRepository).saveAll(any());
    }
}
