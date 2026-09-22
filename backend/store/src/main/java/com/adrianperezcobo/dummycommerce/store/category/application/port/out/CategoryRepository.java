package com.adrianperezcobo.dummycommerce.store.category.application.port.out;

import com.adrianperezcobo.dummycommerce.store.category.domain.Category;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository {

    Optional<Category> findById(UUID id);

    List<Category> findAll();

    boolean existsById(UUID id);

    Category save(Category category);
}