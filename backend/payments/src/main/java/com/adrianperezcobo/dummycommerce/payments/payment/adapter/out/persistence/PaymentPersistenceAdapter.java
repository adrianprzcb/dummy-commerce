package com.adrianperezcobo.dummycommerce.payments.payment.adapter.out.persistence;

import com.adrianperezcobo.dummycommerce.payments.payment.application.port.out.PaymentRepository;
import com.adrianperezcobo.dummycommerce.payments.payment.domain.Payment;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class PaymentPersistenceAdapter
        implements PaymentRepository {

    private final PaymentJpaRepository repository;

    public PaymentPersistenceAdapter(
            PaymentJpaRepository repository
    ) {
        this.repository = repository;
    }

    @Override
    public Payment save(
            Payment payment
    ) {
        PaymentJpaEntity entity =
                new PaymentJpaEntity(
                        payment.getId(),
                        payment.getOrderId(),
                        payment.getAmount(),
                        payment.getCurrency(),
                        payment.getStatus(),
                        payment.getCreatedAt(),
                        payment.getUpdatedAt()
                );

        return toDomain(
                repository.save(entity)
        );
    }

    @Override
    public Optional<Payment> findById(
            UUID paymentId
    ) {
        return repository
                .findById(paymentId)
                .map(this::toDomain);
    }

    @Override
    public Optional<Payment> findByIdForUpdate(
            UUID paymentId
    ) {
        return repository
                .findByIdForUpdate(paymentId)
                .map(this::toDomain);
    }

    @Override
    public Optional<Payment> findByOrderId(
            UUID orderId
    ) {
        return repository
                .findByOrderId(orderId)
                .map(this::toDomain);
    }

    @Override
    public Optional<Payment> findByOrderIdForUpdate(
            UUID orderId
    ) {
        return repository
                .findByOrderIdForUpdate(orderId)
                .map(this::toDomain);
    }

    @Override
    public boolean existsByOrderId(
            UUID orderId
    ) {
        return repository.existsByOrderId(
                orderId
        );
    }

    private Payment toDomain(
            PaymentJpaEntity entity
    ) {
        return new Payment(
                entity.getId(),
                entity.getOrderId(),
                entity.getAmount(),
                entity.getCurrency(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}