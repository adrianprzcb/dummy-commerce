package com.adrianperezcobo.dummycommerce.inventory.reservation.application.integration.inventory;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record StockConfirmationFailedEventV1(UUID messageId, UUID orderId, Instant occurredAt, String reason) {
    public StockConfirmationFailedEventV1 {
        Objects.requireNonNull(messageId, "messageId");
        Objects.requireNonNull(orderId, "orderId");
        Objects.requireNonNull(occurredAt, "occurredAt");
        Objects.requireNonNull(reason, "reason");
    }
}
