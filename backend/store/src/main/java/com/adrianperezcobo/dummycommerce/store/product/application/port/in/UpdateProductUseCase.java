package com.adrianperezcobo.dummycommerce.store.product.application.port.in;

import com.adrianperezcobo.dummycommerce.store.product.application.command.UpdateProductCommand;
import com.adrianperezcobo.dummycommerce.store.product.domain.Product;

import java.util.UUID;

public interface UpdateProductUseCase {

    Product update(UUID id, UpdateProductCommand command);

}