package com.adrianperezcobo.dummycommerce.inventory.reservation.adapter.out.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface StockReservationJpaRepository
        extends JpaRepository<StockReservationJpaEntity, UUID> {

    Optional<StockReservationJpaEntity>
    findByOrderIdAndProductId(
            UUID orderId,
            UUID productId
    );

    boolean existsByOrderIdAndProductId(
            UUID orderId,
            UUID productId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT r
            FROM StockReservationJpaEntity r
            WHERE r.id = :reservationId
            """)
    Optional<StockReservationJpaEntity> findByIdForUpdate(
            @Param("reservationId")
            UUID reservationId
    );
}