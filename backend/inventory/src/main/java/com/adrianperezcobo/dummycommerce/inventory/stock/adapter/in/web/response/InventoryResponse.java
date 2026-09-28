package com.adrianperezcobo.dummycommerce.inventory.stock.adapter.in.web.response;

import java.util.UUID;

public record InventoryResponse(
        UUID productId,
        int availableQuantity,
        int reservedQuantity,
        int totalQuantity
) {
}