package com.adrianperezcobo.dummycommerce.payments.payment.application.exception;

import java.util.UUID;

public class PaymentRefundFailedException
        extends RuntimeException {

    public PaymentRefundFailedException(
            UUID paymentId
    ) {
        super(
                "Refund failed for payment: " +
                        paymentId
        );
    }
}