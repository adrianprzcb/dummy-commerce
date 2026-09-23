package com.adrianperezcobo.dummycommerce.store.product.adapter.out.persistence;

import com.adrianperezcobo.dummycommerce.store.category.adapter.out.persistence.CategoryJpaEntity;
import com.adrianperezcobo.dummycommerce.store.category.adapter.out.persistence.CategoryJpaRepository;
import com.adrianperezcobo.dummycommerce.store.product.domain.ProductStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.NONE;

@DataJpaTest(
        properties = "spring.jpa.hibernate.ddl-auto=validate"
)
@AutoConfigureTestDatabase(replace = NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@Testcontainers
class ProductDatabaseConstraintIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    private ProductJpaRepository productJpaRepository;

    @Autowired
    private CategoryJpaRepository categoryJpaRepository;

    @Test
    void shouldRejectDuplicatedImagePosition() {
        CategoryJpaEntity category =
                categoryJpaRepository.save(
                        new CategoryJpaEntity(
                                UUID.randomUUID(),
                                "Electronics"
                        )
                );

        ProductJpaEntity product = new ProductJpaEntity(
                UUID.randomUUID(),
                "Keyboard",
                "Mechanical keyboard",
                new BigDecimal("99.99"),
                ProductStatus.ACTIVE,
                category
        );

        product.addImage(
                new ProductImageJpaEntity(
                        UUID.randomUUID(),
                        "products/test/first",
                        "Front",
                        0,
                        true
                )
        );

        product.addImage(
                new ProductImageJpaEntity(
                        UUID.randomUUID(),
                        "products/test/second",
                        "Back",
                        0,
                        false
                )
        );

        assertThatThrownBy(() ->
                productJpaRepository.saveAndFlush(product)
        ).isInstanceOf(DataIntegrityViolationException.class);
    }
}