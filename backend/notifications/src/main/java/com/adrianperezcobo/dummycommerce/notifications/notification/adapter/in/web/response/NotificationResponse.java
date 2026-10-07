package com.adrianperezcobo.dummycommerce.notifications.notification.adapter.in.web.response;

import com.adrianperezcobo.dummycommerce.notifications.notification.domain.NotificationChannel;
import com.adrianperezcobo.dummycommerce.notifications.notification.domain.NotificationStatus;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        UUID sourceEventId,
        UUID userId,
        UUID orderId,
        NotificationChannel channel,
        String recipient,
        String subject,
        String message,
        NotificationStatus status,
        Instant createdAt,
        Instant sentAt
) {
}