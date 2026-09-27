package com.adrianperezcobo.dummycommerce.inventory.reservation.application.command;

import java.util.UUID;

public record ReserveStockCommand(
        UUID orderId,
        UUID productId,
        int quantity
) {
}