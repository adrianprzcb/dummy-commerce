package com.adrianperezcobo.dummycommerce.inventory.stock.application.command;

import java.util.UUID;

public record CreateInventoryItemCommand(
        UUID productId,
        int initialQuantity
) {
}