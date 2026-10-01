package com.adrianperezcobo.dummycommerce.payments.payment.application.command;

import java.math.BigDecimal;
import java.util.UUID;

public record ProcessPaymentCommand(
        UUID orderId,
        BigDecimal amount,
        String currency
) {
}