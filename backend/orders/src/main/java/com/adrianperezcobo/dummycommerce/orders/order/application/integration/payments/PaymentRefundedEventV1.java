package com.adrianperezcobo.dummycommerce.orders.order.application.integration.payments;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.math.BigDecimal;

public record PaymentRefundedEventV1(UUID messageId, UUID orderId, UUID paymentId, Instant occurredAt, BigDecimal amount, String currency) {
    public PaymentRefundedEventV1 {
        Objects.requireNonNull(messageId, "messageId");
        Objects.requireNonNull(orderId, "orderId");
        Objects.requireNonNull(paymentId, "paymentId");
        Objects.requireNonNull(occurredAt, "occurredAt");
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(currency, "currency");
    }
}
