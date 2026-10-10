package com.adrianperezcobo.dummycommerce.orders.order.application.integration.inventory;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.List;

public record ReserveStockCommandV1(UUID messageId, UUID orderId, Instant occurredAt, List<Item> items) {
    public ReserveStockCommandV1 {
        Objects.requireNonNull(messageId, "messageId");
        Objects.requireNonNull(orderId, "orderId");
        Objects.requireNonNull(occurredAt, "occurredAt");
        Objects.requireNonNull(items, "items");
        items = List.copyOf(items);
        if (items.isEmpty() || items.stream().map(Item::productId).distinct().count() != items.size()) {
            throw new IllegalArgumentException("Stock command needs nonempty, distinct products");
        }
    }

    public record Item(UUID productId, int quantity) {
        public Item {
            Objects.requireNonNull(productId);
            if (quantity <= 0) throw new IllegalArgumentException("Quantity must be positive");
        }
    }
}
