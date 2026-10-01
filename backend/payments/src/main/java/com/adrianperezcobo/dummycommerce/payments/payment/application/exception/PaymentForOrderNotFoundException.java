package com.adrianperezcobo.dummycommerce.payments.payment.application.exception;

import java.util.UUID;

public class PaymentForOrderNotFoundException
        extends RuntimeException {

    public PaymentForOrderNotFoundException(
            UUID orderId
    ) {
        super(
                "Payment not found for order: " +
                        orderId
        );
    }
}