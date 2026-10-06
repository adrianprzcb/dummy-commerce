package com.adrianperezcobo.dummycommerce.notifications.notification.application.port.in;

import com.adrianperezcobo.dummycommerce.notifications.notification.domain.Notification;

import java.util.List;
import java.util.UUID;

public interface GetUserNotificationsUseCase {

    List<Notification> getByUserId(
            UUID userId
    );
}