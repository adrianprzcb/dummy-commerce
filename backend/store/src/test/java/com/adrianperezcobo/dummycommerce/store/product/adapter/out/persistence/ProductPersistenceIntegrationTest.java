package com.adrianperezcobo.dummycommerce.store.product.adapter.out.persistence;

import com.adrianperezcobo.dummycommerce.store.category.adapter.out.persistence.CategoryJpaEntity;
import com.adrianperezcobo.dummycommerce.store.category.adapter.out.persistence.CategoryJpaRepository;
import com.adrianperezcobo.dummycommerce.store.product.application.port.out.ProductRepository;
import com.adrianperezcobo.dummycommerce.store.product.domain.Product;
import com.adrianperezcobo.dummycommerce.store.product.domain.ProductImage;
import com.adrianperezcobo.dummycommerce.store.product.domain.ProductStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.NONE;

@DataJpaTest(
        properties = "spring.jpa.hibernate.ddl-auto=validate"
)
@AutoConfigureTestDatabase(replace = NONE)
@Import({
        ProductPersistenceAdapter.class,
        ProductPersistenceMapper.class
})
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@Testcontainers
class ProductPersistenceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryJpaRepository categoryJpaRepository;

    private UUID categoryId;

    @BeforeEach
    void createCategory() {
        categoryId = UUID.randomUUID();

        categoryJpaRepository.save(
                new CategoryJpaEntity(
                        categoryId,
                        "Electronics-" + categoryId
                )
        );
    }

    @Test
    void shouldPersistAndLoadProduct() {
        Product product = new Product(
                UUID.randomUUID(),
                "Keyboard",
                "Mechanical keyboard",
                new BigDecimal("99.99"),
                ProductStatus.ACTIVE,
                categoryId
        );

        productRepository.save(product);

        Product loaded = productRepository
                .findById(product.getId())
                .orElseThrow();

        assertThat(loaded.getId())
                .isEqualTo(product.getId());

        assertThat(loaded.getName())
                .isEqualTo("Keyboard");

        assertThat(loaded.getPrice())
                .isEqualByComparingTo("99.99");

        assertThat(loaded.getStatus())
                .isEqualTo(ProductStatus.ACTIVE);

        assertThat(loaded.getCategoryId())
                .isEqualTo(categoryId);
    }

    @Test
    void shouldPersistProductImages() {
        Product product = new Product(
                UUID.randomUUID(),
                "Keyboard",
                "Mechanical keyboard",
                new BigDecimal("99.99"),
                ProductStatus.ACTIVE,
                categoryId
        );

        ProductImage image = new ProductImage(
                UUID.randomUUID(),
                "products/" + product.getId() + "/front",
                "Front view",
                0,
                true
        );

        product.addImage(image);

        productRepository.save(product);

        Product loaded = productRepository
                .findById(product.getId())
                .orElseThrow();

        assertThat(loaded.getImages()).hasSize(1);

        assertThat(loaded.getImages().getFirst().getObjectKey())
                .isEqualTo(image.getObjectKey());

        assertThat(loaded.getImages().getFirst().isPrimary())
                .isTrue();
    }
}