package com.adrianperezcobo.dummycommerce.store.product.domain;

import com.adrianperezcobo.dummycommerce.store.product.domain.ProductImage;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductImageTest {

    @Test
    void shouldCreateValidProductImage() {
        UUID id = UUID.randomUUID();

        ProductImage image = new ProductImage(
                id,
                "products/123/image.webp",
                "Front view",
                0,
                true
        );

        assertThat(image.getId()).isEqualTo(id);
        assertThat(image.getObjectKey())
                .isEqualTo("products/123/image.webp");
        assertThat(image.getPosition()).isZero();
        assertThat(image.isPrimary()).isTrue();
    }

    @Test
    void shouldRejectBlankObjectKey() {
        assertThatThrownBy(() ->
                new ProductImage(
                        UUID.randomUUID(),
                        " ",
                        "Front view",
                        0,
                        false
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Object key cannot be empty");
    }

    @Test
    void shouldRejectNegativePosition() {
        assertThatThrownBy(() ->
                new ProductImage(
                        UUID.randomUUID(),
                        "products/123/image.webp",
                        "Front view",
                        -1,
                        false
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Position cannot be negative");
    }

    @Test
    void shouldChangePrimaryState() {
        ProductImage image = new ProductImage(
                UUID.randomUUID(),
                "products/123/image.webp",
                "Front view",
                0,
                false
        );

        image.makePrimary();

        assertThat(image.isPrimary()).isTrue();

        image.removePrimary();

        assertThat(image.isPrimary()).isFalse();
    }

    @Test
    void shouldChangePosition() {
        ProductImage image = new ProductImage(
                UUID.randomUUID(),
                "products/123/image.webp",
                "Front view",
                0,
                false
        );

        image.changePosition(3);

        assertThat(image.getPosition()).isEqualTo(3);
    }

    @Test
    void shouldRejectNegativePositionChange() {
        ProductImage image = new ProductImage(
                UUID.randomUUID(),
                "products/123/image.webp",
                "Front view",
                0,
                false
        );

        assertThatThrownBy(() -> image.changePosition(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Position cannot be negative");
    }
}