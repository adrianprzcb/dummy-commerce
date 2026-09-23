package com.adrianperezcobo.dummycommerce.store.product.application.service;

import com.adrianperezcobo.dummycommerce.store.category.application.exception.CategoryNotFoundException;
import com.adrianperezcobo.dummycommerce.store.category.application.port.out.CategoryRepository;
import com.adrianperezcobo.dummycommerce.store.product.application.command.CreateProductCommand;
import com.adrianperezcobo.dummycommerce.store.product.application.command.UpdateProductCommand;
import com.adrianperezcobo.dummycommerce.store.product.application.exception.ProductNotFoundException;
import com.adrianperezcobo.dummycommerce.store.product.application.port.out.ProductRepository;
import com.adrianperezcobo.dummycommerce.store.product.domain.Product;
import com.adrianperezcobo.dummycommerce.store.product.domain.ProductStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void shouldCreateProduct() {
        UUID categoryId = UUID.randomUUID();

        CreateProductCommand command = new CreateProductCommand(
                "Keyboard",
                "Mechanical keyboard",
                new BigDecimal("99.99"),
                categoryId
        );

        when(categoryRepository.existsById(categoryId))
                .thenReturn(true);

        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Product result = productService.create(command);

        assertThat(result.getId()).isNotNull();
        assertThat(result.getName()).isEqualTo("Keyboard");
        assertThat(result.getPrice()).isEqualByComparingTo("99.99");
        assertThat(result.getCategoryId()).isEqualTo(categoryId);
        assertThat(result.getStatus()).isEqualTo(ProductStatus.ACTIVE);

        verify(productRepository).save(any(Product.class));
    }

    @Test
    void shouldRejectProductWithUnknownCategory() {
        UUID categoryId = UUID.randomUUID();

        CreateProductCommand command = new CreateProductCommand(
                "Keyboard",
                "Mechanical keyboard",
                new BigDecimal("99.99"),
                categoryId
        );

        when(categoryRepository.existsById(categoryId))
                .thenReturn(false);

        assertThatThrownBy(() -> productService.create(command))
                .isInstanceOf(CategoryNotFoundException.class);

        verify(productRepository, never()).save(any());
    }

    @Test
    void shouldReturnExistingProduct() {
        Product product = createProduct();

        when(productRepository.findById(product.getId()))
                .thenReturn(Optional.of(product));

        Product result = productService.getById(product.getId());

        assertThat(result).isSameAs(product);
    }

    @Test
    void shouldThrowWhenProductDoesNotExist() {
        UUID productId = UUID.randomUUID();

        when(productRepository.findById(productId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getById(productId))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void shouldUpdateProduct() {
        Product product = createProduct();

        UUID newCategoryId = UUID.randomUUID();

        UpdateProductCommand command = new UpdateProductCommand(
                "Updated keyboard",
                "Updated description",
                new BigDecimal("129.99"),
                newCategoryId
        );

        when(productRepository.findById(product.getId()))
                .thenReturn(Optional.of(product));

        when(categoryRepository.existsById(newCategoryId))
                .thenReturn(true);

        when(productRepository.save(product))
                .thenReturn(product);

        Product result = productService.update(
                product.getId(),
                command
        );

        assertThat(result.getName())
                .isEqualTo("Updated keyboard");

        assertThat(result.getDescription())
                .isEqualTo("Updated description");

        assertThat(result.getPrice())
                .isEqualByComparingTo("129.99");

        assertThat(result.getCategoryId())
                .isEqualTo(newCategoryId);
    }

    @Test
    void shouldDiscontinueProduct() {
        Product product = createProduct();

        when(productRepository.findById(product.getId()))
                .thenReturn(Optional.of(product));

        when(productRepository.save(product))
                .thenReturn(product);

        Product result =
                productService.discontinue(product.getId());

        assertThat(result.getStatus())
                .isEqualTo(ProductStatus.DISCONTINUED);
    }

    private Product createProduct() {
        return new Product(
                UUID.randomUUID(),
                "Keyboard",
                "Mechanical keyboard",
                new BigDecimal("99.99"),
                ProductStatus.ACTIVE,
                UUID.randomUUID()
        );
    }
}