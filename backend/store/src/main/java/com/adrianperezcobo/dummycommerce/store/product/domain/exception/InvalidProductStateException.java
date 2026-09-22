package com.adrianperezcobo.dummycommerce.store.product.domain.exception;

public class InvalidProductStateException extends RuntimeException {

    public InvalidProductStateException(String message) {
        super(message);
    }
}