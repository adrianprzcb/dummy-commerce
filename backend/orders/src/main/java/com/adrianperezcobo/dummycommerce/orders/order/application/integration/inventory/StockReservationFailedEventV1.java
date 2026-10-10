package com.adrianperezcobo.dummycommerce.orders.order.application.integration.inventory;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record StockReservationFailedEventV1(UUID messageId, UUID orderId, Instant occurredAt, String reason) {
    public StockReservationFailedEventV1 {
        Objects.requireNonNull(messageId, "messageId");
        Objects.requireNonNull(orderId, "orderId");
        Objects.requireNonNull(occurredAt, "occurredAt");
        Objects.requireNonNull(reason, "reason");
    }
}
