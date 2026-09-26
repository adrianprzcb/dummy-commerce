package com.adrianperezcobo.dummycommerce.inventory.stock.application.command;

import java.util.UUID;

public record ChangeStockCommand(
        UUID productId,
        int quantity
) {
}