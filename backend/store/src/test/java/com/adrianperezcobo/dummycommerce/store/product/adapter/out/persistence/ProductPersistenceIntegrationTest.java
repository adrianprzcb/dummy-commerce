package com.adrianperezcobo.dummycommerce.store.product.adapter.out.persistence;

import com.adrianperezcobo.dummycommerce.store.category.adapter.out.persistence.CategoryJpaEntity;
import com.adrianperezcobo.dummycommerce.store.category.adapter.out.persistence.CategoryJpaRepository;
import com.adrianperezcobo.dummycommerce.store.product.application.port.out.ProductRepository;
import com.adrianperezcobo.dummycommerce.store.product.application.port.out.ProductImageStoragePort;
import com.adrianperezcobo.dummycommerce.store.product.application.service.ProductImageService;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.NONE;

@DataJpaTest(
        properties = "spring.jpa.hibernate.ddl-auto=validate"
)
@AutoConfigureTestDatabase(replace = NONE)
@Import({
        ProductPersistenceAdapter.class,
        ProductPersistenceMapper.class,
        ProductImageService.class
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

    @Autowired
    private ProductImageService imageService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockitoBean
    private ProductImageStoragePort imageStorage;

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

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void shouldRemoveImageWithoutAnExistingCallerTransaction() {
        var transaction = new TransactionTemplate(transactionManager);
        Product product = new Product(UUID.randomUUID(), "Image removal", "",
                new BigDecimal("10.00"), ProductStatus.ACTIVE, categoryId);
        ProductImage image = new ProductImage(UUID.randomUUID(),
                "products/" + product.getId() + "/front", "Front", 0, true);
        product.addImage(image);
        transaction.executeWithoutResult(status -> productRepository.save(product));

        imageService.remove(product.getId(), image.getId());

        Product loaded = transaction.execute(status -> productRepository.findById(product.getId()).orElseThrow());
        assertThat(loaded.getImages()).isEmpty();
        verify(imageStorage).delete(image.getObjectKey());
    }
}
