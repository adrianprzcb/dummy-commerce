package com.adrianperezcobo.dummycommerce.inventory.reservation.application.exception;

import java.util.UUID;

public class ReservationAlreadyExistsException extends RuntimeException {

    public ReservationAlreadyExistsException(
            UUID orderId,
            UUID productId
    ) {
        super(
                "Reservation already exists for order " +
                        orderId +
                        " and product " +
                        productId
        );
    }
}