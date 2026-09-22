package com.adrianperezcobo.dummycommerce.store.category.application.port.in;

import com.adrianperezcobo.dummycommerce.store.category.application.command.CreateCategoryCommand;
import com.adrianperezcobo.dummycommerce.store.category.domain.Category;

public interface CreateCategoryUseCase {

    Category create(CreateCategoryCommand command);
}