package com.adrianperezcobo.dummycommerce.inventory.reservation.application.integration.inventory;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record StockReservedEventV1(UUID messageId, UUID orderId, Instant occurredAt) {
    public StockReservedEventV1 {
        Objects.requireNonNull(messageId, "messageId");
        Objects.requireNonNull(orderId, "orderId");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
