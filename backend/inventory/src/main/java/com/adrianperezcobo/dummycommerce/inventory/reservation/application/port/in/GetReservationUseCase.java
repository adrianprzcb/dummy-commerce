package com.adrianperezcobo.dummycommerce.inventory.reservation.application.port.in;

import com.adrianperezcobo.dummycommerce.inventory.reservation.domain.StockReservation;

import java.util.UUID;

public interface GetReservationUseCase {

    StockReservation getById(UUID reservationId);
}