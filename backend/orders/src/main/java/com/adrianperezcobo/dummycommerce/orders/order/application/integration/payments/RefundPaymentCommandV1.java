package com.adrianperezcobo.dummycommerce.orders.order.application.integration.payments;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record RefundPaymentCommandV1(UUID messageId, UUID orderId, Instant occurredAt) {
    public RefundPaymentCommandV1 {
        Objects.requireNonNull(messageId, "messageId");
        Objects.requireNonNull(orderId, "orderId");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
