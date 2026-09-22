package com.adrianperezcobo.dummycommerce.store.product.adapter.in.web.response;

import java.util.UUID;

public record PrepareProductImageUploadResponse(
        UUID imageId,
        String uploadUrl
) {
}