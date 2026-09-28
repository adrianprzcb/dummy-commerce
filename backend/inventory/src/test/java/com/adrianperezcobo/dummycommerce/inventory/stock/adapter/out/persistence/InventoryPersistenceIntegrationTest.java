package com.adrianperezcobo.dummycommerce.inventory.stock.adapter.out.persistence;

import com.adrianperezcobo.dummycommerce.inventory.stock.application.port.out.InventoryRepository;
import com.adrianperezcobo.dummycommerce.inventory.stock.domain.InventoryItem;
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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.NONE;

@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@Import(InventoryPersistenceAdapter.class)
@Testcontainers
class InventoryPersistenceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer(
                    "postgres:17-alpine"
            );

    @Autowired
    private InventoryRepository inventoryRepository;

    @Test
    void shouldPersistAndLoadInventoryItem() {
        UUID productId = UUID.randomUUID();

        InventoryItem item =
                new InventoryItem(
                        productId,
                        10,
                        2
                );

        inventoryRepository.save(item);

        InventoryItem loaded =
                inventoryRepository
                        .findByProductId(productId)
                        .orElseThrow();

        assertThat(loaded.getProductId())
                .isEqualTo(productId);

        assertThat(loaded.getAvailableQuantity())
                .isEqualTo(10);

        assertThat(loaded.getReservedQuantity())
                .isEqualTo(2);

        assertThat(loaded.getTotalQuantity())
                .isEqualTo(12);
    }

    @Test
    void shouldReportWhetherInventoryExists() {
        UUID productId = UUID.randomUUID();

        assertThat(
                inventoryRepository.existsByProductId(
                        productId
                )
        ).isFalse();

        inventoryRepository.save(
                new InventoryItem(
                        productId,
                        5,
                        0
                )
        );

        assertThat(
                inventoryRepository.existsByProductId(
                        productId
                )
        ).isTrue();
    }

    @Test
    void shouldLoadInventoryItemForUpdate() {
        UUID productId = UUID.randomUUID();

        inventoryRepository.save(
                new InventoryItem(
                        productId,
                        8,
                        2
                )
        );

        InventoryItem loaded =
                inventoryRepository
                        .findByProductIdForUpdate(
                                productId
                        )
                        .orElseThrow();

        assertThat(loaded.getAvailableQuantity())
                .isEqualTo(8);

        assertThat(loaded.getReservedQuantity())
                .isEqualTo(2);
    }
}