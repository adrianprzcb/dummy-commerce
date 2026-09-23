package com.adrianperezcobo.dummycommerce.store.product.application.port.in;

import java.util.UUID;

public interface RemoveProductImageUseCase {

    void remove(UUID productId, UUID imageId);
}