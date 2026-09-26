package com.adrianperezcobo.dummycommerce.inventory.reservation.domain.exception;

public class InvalidReservationStateException
        extends RuntimeException {

    public InvalidReservationStateException(
            String message
    ) {
        super(message);
    }
}