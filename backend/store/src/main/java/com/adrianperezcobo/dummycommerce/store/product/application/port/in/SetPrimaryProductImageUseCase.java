package com.adrianperezcobo.dummycommerce.store.product.application.port.in;

import com.adrianperezcobo.dummycommerce.store.product.domain.Product;

import java.util.UUID;

public interface SetPrimaryProductImageUseCase {

    Product setPrimary(UUID productId, UUID imageId);
}