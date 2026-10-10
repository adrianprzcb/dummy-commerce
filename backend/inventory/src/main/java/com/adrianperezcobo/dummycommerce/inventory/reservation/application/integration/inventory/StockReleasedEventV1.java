package com.adrianperezcobo.dummycommerce.inventory.reservation.application.integration.inventory;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record StockReleasedEventV1(UUID messageId, UUID orderId, Instant occurredAt) {
    public StockReleasedEventV1 {
        Objects.requireNonNull(messageId, "messageId");
        Objects.requireNonNull(orderId, "orderId");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
