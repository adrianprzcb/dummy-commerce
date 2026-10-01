package com.adrianperezcobo.dummycommerce.payments.payment.application.exception;

import java.util.UUID;

public class PaymentNotFoundException
        extends RuntimeException {

    public PaymentNotFoundException(
            UUID paymentId
    ) {
        super(
                "Payment not found: " +
                        paymentId
        );
    }
}