package com.adrianperezcobo.dummycommerce.notifications.notification.application.command;

import com.adrianperezcobo.dummycommerce.notifications.notification.domain.NotificationChannel;

import java.util.UUID;

public record CreateNotificationCommand(
        UUID sourceEventId,
        UUID userId,
        UUID orderId,
        NotificationChannel channel,
        String recipient,
        String subject,
        String message
) {
}