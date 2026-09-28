package com.adrianperezcobo.dummycommerce.inventory.reservation.application.port.in;

import com.adrianperezcobo.dummycommerce.inventory.reservation.domain.StockReservation;

import java.util.UUID;

public interface ReleaseReservationUseCase {

    StockReservation release(UUID reservationId);
}