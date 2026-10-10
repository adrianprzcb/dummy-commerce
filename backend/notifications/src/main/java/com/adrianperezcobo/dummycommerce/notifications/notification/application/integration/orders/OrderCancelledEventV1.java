package com.adrianperezcobo.dummycommerce.notifications.notification.application.integration.orders;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record OrderCancelledEventV1(UUID messageId, UUID orderId, UUID userId, Instant occurredAt, String reason) {
    public OrderCancelledEventV1 {
        Objects.requireNonNull(messageId, "messageId");
        Objects.requireNonNull(orderId, "orderId");
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(occurredAt, "occurredAt");
        Objects.requireNonNull(reason, "reason");
    }
}
