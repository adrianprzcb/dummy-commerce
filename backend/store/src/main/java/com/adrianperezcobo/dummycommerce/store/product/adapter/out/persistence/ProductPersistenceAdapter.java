package com.adrianperezcobo.dummycommerce.store.product.adapter.out.persistence;

import com.adrianperezcobo.dummycommerce.store.category.adapter.out.persistence.CategoryJpaEntity;
import com.adrianperezcobo.dummycommerce.store.category.adapter.out.persistence.CategoryJpaRepository;
import com.adrianperezcobo.dummycommerce.store.category.application.exception.CategoryNotFoundException;
import com.adrianperezcobo.dummycommerce.store.product.application.port.out.ProductRepository;
import com.adrianperezcobo.dummycommerce.store.product.domain.Product;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class ProductPersistenceAdapter implements ProductRepository {

    private final ProductJpaRepository productJpaRepository;
    private final CategoryJpaRepository categoryJpaRepository;
    private final ProductPersistenceMapper mapper;

    public ProductPersistenceAdapter(
            ProductJpaRepository productJpaRepository,
            CategoryJpaRepository categoryJpaRepository,
            ProductPersistenceMapper mapper
    ) {
        this.productJpaRepository = productJpaRepository;
        this.categoryJpaRepository = categoryJpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Product save(Product product) {

        CategoryJpaEntity category = categoryJpaRepository
                .findById(product.getCategoryId())
                .orElseThrow(() ->
                        new CategoryNotFoundException(product.getCategoryId())
                );

        ProductJpaEntity entity = mapper.toEntity(product, category);

        return mapper.toDomain(productJpaRepository.save(entity));
    }

    @Override
    public Optional<Product> findById(UUID id) {
        return productJpaRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public List<Product> findAll() {
        return productJpaRepository.findAll()
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsById(UUID id) {
        return productJpaRepository.existsById(id);
    }
}