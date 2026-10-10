package com.adrianperezcobo.dummycommerce.orders.order.application.integration.payments;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.math.BigDecimal;

public record ProcessPaymentCommandV1(UUID messageId, UUID orderId, Instant occurredAt, BigDecimal amount, String currency) {
    public ProcessPaymentCommandV1 {
        Objects.requireNonNull(messageId, "messageId");
        Objects.requireNonNull(orderId, "orderId");
        Objects.requireNonNull(occurredAt, "occurredAt");
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(currency, "currency");
    }
}
