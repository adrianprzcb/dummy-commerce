package com.adrianperezcobo.dummycommerce.store.product.application.port.in;

import com.adrianperezcobo.dummycommerce.store.product.domain.Product;

import java.util.List;
import java.util.UUID;

public interface GetProductUseCase {

    Product getById(UUID id);

    List<Product> getAll();
}