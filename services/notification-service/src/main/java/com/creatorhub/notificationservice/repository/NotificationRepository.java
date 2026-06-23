package com.creatorhub.notificationservice.repository;

import com.creatorhub.notificationservice.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface NotificationRepository extends MongoRepository<Notification, String> {

    /** A recipient's notifications (sort is controlled by the supplied Pageable). */
    Page<Notification> findByRecipientId(Long recipientId, Pageable pageable);

    java.util.List<Notification> findByRecipientIdAndReadFalse(Long recipientId);

    long countByRecipientIdAndReadFalse(Long recipientId);
}
