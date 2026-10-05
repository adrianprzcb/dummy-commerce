package com.adrianperezcobo.dummycommerce.notifications.notification.application.port.out;

import com.adrianperezcobo.dummycommerce.notifications.notification.domain.Notification;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository {

    Notification save(
            Notification notification
    );

    Optional<Notification> findById(
            UUID notificationId
    );

    Optional<Notification> findByIdForUpdate(
            UUID notificationId
    );

    Optional<Notification> findBySourceEventId(
            UUID sourceEventId
    );

    Optional<Notification> findBySourceEventIdForUpdate(
            UUID sourceEventId
    );

    boolean existsBySourceEventId(
            UUID sourceEventId
    );

    List<Notification> findByUserId(
            UUID userId
    );
}