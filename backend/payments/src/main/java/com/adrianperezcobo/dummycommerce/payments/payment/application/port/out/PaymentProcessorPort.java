package com.adrianperezcobo.dummycommerce.payments.payment.application.port.out;

import java.math.BigDecimal;
import java.util.UUID;

public interface PaymentProcessorPort {

    PaymentProcessorResult process(
            UUID paymentId,
            UUID orderId,
            BigDecimal amount,
            String currency
    );

    PaymentProcessorResult refund(
            UUID paymentId,
            UUID orderId,
            BigDecimal amount,
            String currency
    );
}