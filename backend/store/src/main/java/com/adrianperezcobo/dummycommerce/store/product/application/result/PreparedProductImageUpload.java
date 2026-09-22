package com.adrianperezcobo.dummycommerce.store.product.application.result;

import java.util.UUID;

public record PreparedProductImageUpload(
        UUID imageId,
        String uploadUrl
) {
}