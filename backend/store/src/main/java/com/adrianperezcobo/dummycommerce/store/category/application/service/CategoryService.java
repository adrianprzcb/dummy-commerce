package com.adrianperezcobo.dummycommerce.store.category.application.service;

import com.adrianperezcobo.dummycommerce.store.category.application.command.CreateCategoryCommand;
import com.adrianperezcobo.dummycommerce.store.category.application.exception.CategoryNotFoundException;
import com.adrianperezcobo.dummycommerce.store.category.application.port.in.CreateCategoryUseCase;
import com.adrianperezcobo.dummycommerce.store.category.application.port.in.GetCategoryUseCase;
import com.adrianperezcobo.dummycommerce.store.category.application.port.out.CategoryRepository;
import com.adrianperezcobo.dummycommerce.store.category.domain.Category;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class CategoryService implements CreateCategoryUseCase, GetCategoryUseCase {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public Category create(CreateCategoryCommand command) {
        Category category = new Category(
                UUID.randomUUID(),
                command.name()
        );

        return categoryRepository.save(category);
    }

    @Override
    @Transactional(readOnly = true)
    public Category getById(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Category> getAll() {
        return categoryRepository.findAll();
    }
}