package com.adrianperezcobo.dummycommerce.store.product.application.command;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateProductCommand(
        String name,
        String description,
        BigDecimal price,
        UUID categoryId
) {
}