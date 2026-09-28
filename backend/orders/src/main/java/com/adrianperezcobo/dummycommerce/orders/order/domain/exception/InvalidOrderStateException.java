package com.adrianperezcobo.dummycommerce.orders.order.domain.exception;

public class InvalidOrderStateException
        extends RuntimeException {

    public InvalidOrderStateException(
            String message
    ) {
        super(message);
    }
}