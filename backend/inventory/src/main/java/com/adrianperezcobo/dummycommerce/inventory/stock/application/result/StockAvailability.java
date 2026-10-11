package com.adrianperezcobo.dummycommerce.inventory.stock.application.result;

import java.util.UUID;

public record StockAvailability(UUID productId, int availableQuantity) {
}
