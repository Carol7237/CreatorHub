package com.creatorhub.notificationservice.service.impl;

import com.creatorhub.common.PageableUtils;
import com.creatorhub.common.Viewer;
import com.creatorhub.common.dto.PagedResponse;
import com.creatorhub.common.exception.ResourceNotFoundException;
import com.creatorhub.notificationservice.dto.CreateNotificationRequest;
import com.creatorhub.notificationservice.dto.NotificationResponse;
import com.creatorhub.notificationservice.dto.mapper.NotificationMapper;
import com.creatorhub.notificationservice.model.Notification;
import com.creatorhub.notificationservice.repository.NotificationRepository;
import com.creatorhub.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private static final Set<String> ALLOWED_SORT = Set.of("id", "createdAt", "read", "type");

    private final NotificationRepository notificationRepository;

    @Override
    public NotificationResponse create(CreateNotificationRequest request) {
        Notification n = new Notification();
        n.setRecipientId(request.getRecipientId());
        n.setType(request.getType());
        n.setMessage(request.getMessage());
        n.setActorId(request.getActorId());
        n.setRelatedId(request.getRelatedId());
        n.setRead(false);
        Notification saved = notificationRepository.save(n);
        log.info("Notification created: id={} recipient={} type={}", saved.getId(), saved.getRecipientId(), saved.getType());
        return NotificationMapper.toResponse(saved);
    }

    @Override
    public PagedResponse<NotificationResponse> findForViewer(Viewer viewer, Pageable pageable) {
        Pageable safe = PageableUtils.sanitize(pageable, ALLOWED_SORT);
        // Default to newest-first when the client did not ask for a specific order.
        if (safe.getSort().isUnsorted()) {
            safe = PageRequest.of(safe.getPageNumber(), safe.getPageSize(), Sort.by(Sort.Direction.DESC, "createdAt"));
        }
        return PagedResponse.from(
                notificationRepository.findByRecipientId(viewer.userId(), safe).map(NotificationMapper::toResponse));
    }

    @Override
    public long unreadCount(Viewer viewer) {
        return notificationRepository.countByRecipientIdAndReadFalse(viewer.userId());
    }

    @Override
    public NotificationResponse markRead(String id, Viewer viewer) {
        Notification n = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", id));
        if (!viewer.isOwnerOrAdmin(n.getRecipientId())) {
            throw new AccessDeniedException("You can only read your own notifications");
        }
        if (!n.isRead()) {
            n.setRead(true);
            n = notificationRepository.save(n);
        }
        return NotificationMapper.toResponse(n);
    }

    @Override
    public long markAllRead(Viewer viewer) {
        List<Notification> unread = notificationRepository.findByRecipientIdAndReadFalse(viewer.userId());
        unread.forEach(n -> n.setRead(true));
        if (!unread.isEmpty()) {
            notificationRepository.saveAll(unread);
        }
        return unread.size();
    }
}
