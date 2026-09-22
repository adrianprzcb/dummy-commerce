package com.adrianperezcobo.dummycommerce.store.category.adapter.out.persistence;

import com.adrianperezcobo.dummycommerce.store.category.application.port.out.CategoryRepository;
import com.adrianperezcobo.dummycommerce.store.category.domain.Category;
import org.springframework.stereotype.Component;
import com.adrianperezcobo.dummycommerce.store.category.adapter.out.persistence.*;
import com.adrianperezcobo.dummycommerce.store.category.adapter.out.persistence.CategoryJpaEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class CategoryPersistenceAdapter implements CategoryRepository {

    private final CategoryJpaRepository categoryJpaRepository;

    public CategoryPersistenceAdapter(
            CategoryJpaRepository categoryJpaRepository
    ) {
        this.categoryJpaRepository = categoryJpaRepository;
    }

    @Override
    public Optional<Category> findById(UUID id) {
        return categoryJpaRepository.findById(id)
                .map(this::toDomain);
    }

    @Override
    public List<Category> findAll() {
        return categoryJpaRepository.findAll()
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public boolean existsById(UUID id) {
        return categoryJpaRepository.existsById(id);
    }

    @Override
    public Category save(Category category) {

        CategoryJpaEntity entity = new CategoryJpaEntity(
                category.getId(),
                category.getName()
        );

        return toDomain(categoryJpaRepository.save(entity));
    }

    private Category toDomain(CategoryJpaEntity entity) {
        return new Category(
                entity.getId(),
                entity.getName()
        );
    }
}