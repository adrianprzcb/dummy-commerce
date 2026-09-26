package com.adrianperezcobo.dummycommerce.inventory.reservation.application.port.out;

import com.adrianperezcobo.dummycommerce.inventory.reservation.domain.StockReservation;

import java.util.Optional;
import java.util.UUID;

public interface StockReservationRepository {

    StockReservation save(
            StockReservation reservation
    );

    Optional<StockReservation> findById(
            UUID reservationId
    );

    Optional<StockReservation> findByIdForUpdate(
            UUID reservationId
    );

    Optional<StockReservation> findByOrderIdAndProductId(
            UUID orderId,
            UUID productId
    );

    boolean existsByOrderIdAndProductId(
            UUID orderId,
            UUID productId
    );
}