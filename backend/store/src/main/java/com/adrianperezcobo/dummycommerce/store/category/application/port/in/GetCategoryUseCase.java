package com.adrianperezcobo.dummycommerce.store.category.application.port.in;

import com.adrianperezcobo.dummycommerce.store.category.domain.Category;

import java.util.List;
import java.util.UUID;

public interface GetCategoryUseCase {

    Category getById(UUID id);

    List<Category> getAll();
}