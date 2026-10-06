package com.adrianperezcobo.dummycommerce.notifications.notification.application.port.out;

import com.adrianperezcobo.dummycommerce.notifications.notification.domain.NotificationChannel;

import java.util.UUID;

public interface NotificationSenderPort {

    NotificationSenderResult send(
            UUID sourceEventId,
            NotificationChannel channel,
            String recipient,
            String subject,
            String message
    );
}