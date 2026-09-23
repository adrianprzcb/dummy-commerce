package com.adrianperezcobo.dummycommerce.store.domain;

import com.adrianperezcobo.dummycommerce.store.product.domain.Product;
import com.adrianperezcobo.dummycommerce.store.product.domain.ProductImage;
import com.adrianperezcobo.dummycommerce.store.product.domain.ProductStatus;
import com.adrianperezcobo.dummycommerce.store.product.domain.exception.InvalidProductStateException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductTest {

    private final UUID categoryId = UUID.randomUUID();

    @Test
    void shouldCreateValidProduct() {
        Product product = createProduct();

        assertThat(product.getName()).isEqualTo("Keyboard");
        assertThat(product.getPrice()).isEqualByComparingTo("99.99");
        assertThat(product.getStatus()).isEqualTo(ProductStatus.ACTIVE);
        assertThat(product.getCategoryId()).isEqualTo(categoryId);
    }

    @Test
    void shouldRejectBlankName() {
        assertThatThrownBy(() ->
                new Product(
                        UUID.randomUUID(),
                        " ",
                        "Description",
                        new BigDecimal("99.99"),
                        ProductStatus.ACTIVE,
                        categoryId
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Product name cannot be empty");
    }

    @Test
    void shouldRejectNegativePrice() {
        assertThatThrownBy(() ->
                new Product(
                        UUID.randomUUID(),
                        "Keyboard",
                        "Description",
                        new BigDecimal("-1.00"),
                        ProductStatus.ACTIVE,
                        categoryId
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Product price cannot be negative");
    }

    @Test
    void firstImageShouldAutomaticallyBecomePrimary() {
        Product product = createProduct();

        ProductImage image = new ProductImage(
                UUID.randomUUID(),
                "products/example/image-1",
                "Front view",
                0,
                false
        );

        product.addImage(image);

        assertThat(product.getImages()).hasSize(1);
        assertThat(image.isPrimary()).isTrue();
    }

    @Test
    void shouldRejectDuplicatedImagePosition() {
        Product product = createProduct();

        product.addImage(
                new ProductImage(
                        UUID.randomUUID(),
                        "products/example/image-1",
                        "Front",
                        0,
                        true
                )
        );

        ProductImage secondImage = new ProductImage(
                UUID.randomUUID(),
                "products/example/image-2",
                "Back",
                0,
                false
        );

        assertThatThrownBy(() -> product.addImage(secondImage))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Image position already exists: 0");
    }

    @Test
    void shouldChangePrimaryImage() {
        Product product = createProduct();

        ProductImage firstImage = new ProductImage(
                UUID.randomUUID(),
                "products/example/image-1",
                "Front",
                0,
                true
        );

        ProductImage secondImage = new ProductImage(
                UUID.randomUUID(),
                "products/example/image-2",
                "Back",
                1,
                false
        );

        product.addImage(firstImage);
        product.addImage(secondImage);

        product.setPrimaryImage(secondImage.getId());

        assertThat(firstImage.isPrimary()).isFalse();
        assertThat(secondImage.isPrimary()).isTrue();
    }

    @Test
    void removingPrimaryImageShouldPromoteLowestPositionImage() {
        Product product = createProduct();

        ProductImage first = new ProductImage(
                UUID.randomUUID(),
                "products/example/image-1",
                "Front",
                0,
                true
        );

        ProductImage second = new ProductImage(
                UUID.randomUUID(),
                "products/example/image-2",
                "Side",
                1,
                false
        );

        ProductImage third = new ProductImage(
                UUID.randomUUID(),
                "products/example/image-3",
                "Back",
                2,
                false
        );

        product.addImage(first);
        product.addImage(second);
        product.addImage(third);

        product.removeImage(first.getId());

        assertThat(second.isPrimary()).isTrue();
        assertThat(third.isPrimary()).isFalse();
    }

    @Test
    void discontinuedProductCannotBeActivated() {
        Product product = createProduct();

        product.discontinue();

        assertThatThrownBy(product::activate)
                .isInstanceOf(InvalidProductStateException.class)
                .hasMessage("A discontinued product cannot be activated");
    }

    private Product createProduct() {
        return new Product(
                UUID.randomUUID(),
                "Keyboard",
                "Mechanical keyboard",
                new BigDecimal("99.99"),
                ProductStatus.ACTIVE,
                categoryId
        );
    }
}