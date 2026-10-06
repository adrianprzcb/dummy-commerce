package com.adrianperezcobo.dummycommerce.notifications.notification.application.port.in;

import com.adrianperezcobo.dummycommerce.notifications.notification.domain.Notification;

import java.util.UUID;

public interface GetNotificationBySourceEventUseCase {

    Notification getBySourceEventId(
            UUID sourceEventId
    );
}