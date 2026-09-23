package com.adrianperezcobo.dummycommerce.store.product.application.port.in;

import java.util.UUID;

public interface GetProductImageContentUseCase {

    String getAccessUrl(UUID productId, UUID imageId);
}