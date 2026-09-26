package com.adrianperezcobo.dummycommerce.inventory.reservation.application.exception;

import java.util.UUID;

public class ReservationNotFoundException extends RuntimeException {

    public ReservationNotFoundException(UUID reservationId) {
        super("Stock reservation not found: " + reservationId);
    }
}