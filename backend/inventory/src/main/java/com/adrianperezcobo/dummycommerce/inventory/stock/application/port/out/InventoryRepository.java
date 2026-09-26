package com.adrianperezcobo.dummycommerce.inventory.stock.application.port.out;

import com.adrianperezcobo.dummycommerce.inventory.stock.domain.InventoryItem;

import java.util.Optional;
import java.util.UUID;

public interface InventoryRepository {

    InventoryItem save(InventoryItem item);

    Optional<InventoryItem> findByProductId(
            UUID productId
    );

    Optional<InventoryItem> findByProductIdForUpdate(
            UUID productId
    );

    boolean existsByProductId(UUID productId);
}