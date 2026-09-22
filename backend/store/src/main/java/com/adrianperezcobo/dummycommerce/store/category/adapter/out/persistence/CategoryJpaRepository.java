package com.adrianperezcobo.dummycommerce.store.category.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import com.adrianperezcobo.dummycommerce.store.category.adapter.out.persistence.CategoryJpaEntity;

import java.util.UUID;

public interface CategoryJpaRepository
        extends JpaRepository<CategoryJpaEntity, UUID> {
}