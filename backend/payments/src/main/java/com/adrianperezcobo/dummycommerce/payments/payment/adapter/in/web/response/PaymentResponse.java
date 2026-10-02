package com.adrianperezcobo.dummycommerce.payments.payment.adapter.in.web.response;

import com.adrianperezcobo.dummycommerce.payments.payment.domain.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID orderId,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}