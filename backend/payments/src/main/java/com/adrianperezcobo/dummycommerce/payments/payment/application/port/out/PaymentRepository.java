package com.adrianperezcobo.dummycommerce.payments.payment.application.port.out;

import com.adrianperezcobo.dummycommerce.payments.payment.domain.Payment;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository {

    Payment save(Payment payment);

    Optional<Payment> findById(
            UUID paymentId
    );

    Optional<Payment> findByIdForUpdate(
            UUID paymentId
    );

    Optional<Payment> findByOrderId(
            UUID orderId
    );

    Optional<Payment> findByOrderIdForUpdate(
            UUID orderId
    );

    boolean existsByOrderId(
            UUID orderId
    );
}