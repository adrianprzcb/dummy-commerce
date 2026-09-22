package com.adrianperezcobo.dummycommerce.store.product.application.port.in;

import com.adrianperezcobo.dummycommerce.store.product.domain.Product;

import java.util.UUID;

public interface ChangeProductStatusUseCase {

    Product activate(UUID id);

    Product deactivate(UUID id);

    Product discontinue(UUID id);
}