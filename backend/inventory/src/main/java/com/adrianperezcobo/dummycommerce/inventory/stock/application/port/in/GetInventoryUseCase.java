package com.adrianperezcobo.dummycommerce.inventory.stock.application.port.in;

import com.adrianperezcobo.dummycommerce.inventory.stock.domain.InventoryItem;

import java.util.UUID;

public interface GetInventoryUseCase {

    InventoryItem getByProductId(UUID productId);
}