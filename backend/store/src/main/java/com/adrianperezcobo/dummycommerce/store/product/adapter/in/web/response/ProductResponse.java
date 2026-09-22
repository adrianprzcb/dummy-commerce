package com.adrianperezcobo.dummycommerce.store.product.adapter.in.web.response;

import com.adrianperezcobo.dummycommerce.store.product.domain.ProductStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        String name,
        String description,
        BigDecimal price,
        ProductStatus status,
        UUID categoryId,
        List<ProductImageResponse> images
) {
}