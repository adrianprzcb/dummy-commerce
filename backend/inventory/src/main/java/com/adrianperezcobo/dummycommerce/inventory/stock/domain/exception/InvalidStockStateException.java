package com.adrianperezcobo.dummycommerce.inventory.stock.domain.exception;

public class InvalidStockStateException
        extends RuntimeException {

    public InvalidStockStateException(String message) {
        super(message);
    }
}