package com.adrianperezcobo.dummycommerce.store.product.application.port.in;

import com.adrianperezcobo.dummycommerce.store.product.application.command.CreateProductCommand;
import com.adrianperezcobo.dummycommerce.store.product.domain.Product;

public interface CreateProductUseCase {

    Product create(CreateProductCommand command);
}