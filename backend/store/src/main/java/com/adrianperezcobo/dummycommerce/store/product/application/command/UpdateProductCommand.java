package com.adrianperezcobo.dummycommerce.store.product.application.command;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdateProductCommand(
        String name,
        String description,
        BigDecimal price,
        UUID categoryId
) {
}