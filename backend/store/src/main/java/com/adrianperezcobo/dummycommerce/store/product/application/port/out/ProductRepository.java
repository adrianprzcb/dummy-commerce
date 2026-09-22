package com.adrianperezcobo.dummycommerce.store.product.application.port.out;

import com.adrianperezcobo.dummycommerce.store.product.domain.Product;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository {

    Product save(Product product);

    Optional<Product> findById(UUID id);

    List<Product> findAll();

    boolean existsById(UUID id);
}