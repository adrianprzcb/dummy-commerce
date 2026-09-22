package com.adrianperezcobo.dummycommerce.store.product.application.service;

import com.adrianperezcobo.dummycommerce.store.category.application.exception.CategoryNotFoundException;
import com.adrianperezcobo.dummycommerce.store.category.application.port.out.CategoryRepository;
import com.adrianperezcobo.dummycommerce.store.product.application.command.CreateProductCommand;
import com.adrianperezcobo.dummycommerce.store.product.application.exception.ProductNotFoundException;
import com.adrianperezcobo.dummycommerce.store.product.application.port.in.CreateProductUseCase;
import com.adrianperezcobo.dummycommerce.store.product.application.port.in.GetProductUseCase;
import com.adrianperezcobo.dummycommerce.store.product.application.port.in.UpdateProductUseCase;
import com.adrianperezcobo.dummycommerce.store.product.application.port.out.ProductRepository;
import com.adrianperezcobo.dummycommerce.store.product.domain.Product;
import com.adrianperezcobo.dummycommerce.store.product.domain.ProductStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.adrianperezcobo.dummycommerce.store.product.application.command.UpdateProductCommand;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ProductService implements CreateProductUseCase, GetProductUseCase, UpdateProductUseCase {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductService(
            ProductRepository productRepository,
            CategoryRepository categoryRepository
    ) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    @Override
    public Product create(CreateProductCommand command) {

        if (!categoryRepository.existsById(command.categoryId())) {
            throw new CategoryNotFoundException(command.categoryId());
        }

        Product product = new Product(
                UUID.randomUUID(),
                command.name(),
                command.description(),
                command.price(),
                ProductStatus.ACTIVE,
                command.categoryId()
        );

        return productRepository.save(product);
    }

    @Override
    public Product update(UUID id, UpdateProductCommand command) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        if (!categoryRepository.existsById(command.categoryId())) {
            throw new CategoryNotFoundException(command.categoryId());
        }

        product.changeName(command.name());
        product.changeDescription(command.description());
        product.changePrice(command.price());
        product.changeCategory(command.categoryId());

        return productRepository.save(product);
    }

    @Override
    @Transactional(readOnly = true)
    public Product getById(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Product> getAll() {
        return productRepository.findAll();
    }
}