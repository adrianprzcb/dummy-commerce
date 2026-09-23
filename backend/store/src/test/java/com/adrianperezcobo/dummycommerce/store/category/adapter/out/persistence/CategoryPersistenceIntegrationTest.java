package com.adrianperezcobo.dummycommerce.store.category.adapter.out.persistence;

import com.adrianperezcobo.dummycommerce.store.category.application.port.out.CategoryRepository;
import com.adrianperezcobo.dummycommerce.store.category.domain.Category;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.NONE;

@DataJpaTest(
        properties = "spring.jpa.hibernate.ddl-auto=validate"
)
@AutoConfigureTestDatabase(replace = NONE)
@Import(CategoryPersistenceAdapter.class)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@Testcontainers
class CategoryPersistenceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void shouldPersistAndLoadCategory() {
        Category category = new Category(
                UUID.randomUUID(),
                "Electronics"
        );

        categoryRepository.save(category);

        Category loaded = categoryRepository
                .findById(category.getId())
                .orElseThrow();

        assertThat(loaded.getId())
                .isEqualTo(category.getId());

        assertThat(loaded.getName())
                .isEqualTo("Electronics");
    }

    @Test
    void shouldReturnAllCategories() {
        categoryRepository.save(
                new Category(UUID.randomUUID(), "Electronics")
        );

        categoryRepository.save(
                new Category(UUID.randomUUID(), "Gaming")
        );

        assertThat(categoryRepository.findAll())
                .extracting(Category::getName)
                .contains("Electronics", "Gaming");
    }
}