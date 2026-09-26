package com.adrianperezcobo.dummycommerce.inventory.stock.application.exception;

import java.util.UUID;

public class InventoryItemAlreadyExistsException extends RuntimeException{

    public InventoryItemAlreadyExistsException(UUID productId){
        super("Inventory item already exists for product: " + productId);
    }
}
