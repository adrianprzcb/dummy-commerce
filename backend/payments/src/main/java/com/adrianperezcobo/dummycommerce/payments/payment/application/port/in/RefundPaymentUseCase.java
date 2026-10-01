package com.adrianperezcobo.dummycommerce.payments.payment.application.port.in;

import com.adrianperezcobo.dummycommerce.payments.payment.domain.Payment;

import java.util.UUID;

public interface RefundPaymentUseCase {

    Payment refund(UUID paymentId);
}