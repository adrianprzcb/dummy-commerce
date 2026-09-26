package com.adrianperezcobo.dummycommerce.inventory.stock.application.exception;

import java.util.UUID;

public class InventoryItemNotFoundException extends RuntimeException {

    public InventoryItemNotFoundException(UUID productId) {
        super("Inventory item not found for product: " + productId);
    }
}