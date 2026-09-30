package com.adrianperezcobo.dummycommerce.payments.payment.adapter.out.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PaymentJpaRepository
        extends JpaRepository<PaymentJpaEntity, UUID> {

    Optional<PaymentJpaEntity> findByOrderId(
            UUID orderId
    );

    boolean existsByOrderId(
            UUID orderId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT p
            FROM PaymentJpaEntity p
            WHERE p.id = :paymentId
            """)
    Optional<PaymentJpaEntity> findByIdForUpdate(
            @Param("paymentId")
            UUID paymentId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT p
            FROM PaymentJpaEntity p
            WHERE p.orderId = :orderId
            """)
    Optional<PaymentJpaEntity> findByOrderIdForUpdate(
            @Param("orderId")
            UUID orderId
    );
}