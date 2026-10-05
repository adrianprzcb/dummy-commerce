package com.adrianperezcobo.dummycommerce.notifications.notification.application.port.out;

import com.adrianperezcobo.dummycommerce.notifications.notification.domain.NotificationChannel;

public interface NotificationSenderPort {

    NotificationSenderResult send(
            NotificationChannel channel,
            String recipient,
            String subject,
            String message
    );
}