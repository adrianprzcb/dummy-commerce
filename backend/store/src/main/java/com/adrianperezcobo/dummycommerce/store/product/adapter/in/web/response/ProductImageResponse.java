package com.adrianperezcobo.dummycommerce.store.product.adapter.in.web.response;

import java.util.UUID;

public record ProductImageResponse(
        UUID id,
        String objectKey,
        String altText,
        int position,
        boolean primary
) {
}