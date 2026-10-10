package com.adrianperezcobo.dummycommerce.notifications.notification.application.integration.orders;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record OrderConfirmedEventV1(UUID messageId, UUID orderId, UUID userId, Instant occurredAt) {
    public OrderConfirmedEventV1 {
        Objects.requireNonNull(messageId, "messageId");
        Objects.requireNonNull(orderId, "orderId");
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
