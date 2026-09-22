package com.adrianperezcobo.dummycommerce.store.category.adapter.in.web;

import com.adrianperezcobo.dummycommerce.store.category.adapter.in.web.request.CreateCategoryRequest;
import com.adrianperezcobo.dummycommerce.store.category.adapter.in.web.response.CategoryResponse;
import com.adrianperezcobo.dummycommerce.store.category.application.command.CreateCategoryCommand;
import com.adrianperezcobo.dummycommerce.store.category.domain.Category;
import org.springframework.stereotype.Component;

@Component
public class CategoryWebMapper {

    public CreateCategoryCommand toCommand(CreateCategoryRequest request) {
        return new CreateCategoryCommand(request.name());
    }

    public CategoryResponse toResponse(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName()
        );
    }
}