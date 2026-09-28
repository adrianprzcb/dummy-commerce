package com.adrianperezcobo.dummycommerce.inventory.shared.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.NONE;

@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@Testcontainers
class InventoryDatabaseConstraintIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer(
                    "postgres:17-alpine"
            );

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldRejectNegativeAvailableQuantity() {
        assertThatThrownBy(() ->
                insertInventory(
                        UUID.randomUUID(),
                        -1,
                        0
                )
        ).isInstanceOf(
                DataIntegrityViolationException.class
        );
    }

    @Test
    void shouldRejectNegativeReservedQuantity() {
        assertThatThrownBy(() ->
                insertInventory(
                        UUID.randomUUID(),
                        10,
                        -1
                )
        ).isInstanceOf(
                DataIntegrityViolationException.class
        );
    }

    @Test
    void shouldRejectNonPositiveReservationQuantity() {
        UUID productId = UUID.randomUUID();

        insertInventory(
                productId,
                10,
                0
        );

        assertThatThrownBy(() ->
                insertReservation(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        productId,
                        0,
                        "RESERVED"
                )
        ).isInstanceOf(
                DataIntegrityViolationException.class
        );
    }

    @Test
    void shouldRejectInvalidReservationStatus() {
        UUID productId = UUID.randomUUID();

        insertInventory(
                productId,
                10,
                0
        );

        assertThatThrownBy(() ->
                insertReservation(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        productId,
                        2,
                        "INVALID_STATUS"
                )
        ).isInstanceOf(
                DataIntegrityViolationException.class
        );
    }

    @Test
    void shouldRejectDuplicatedOrderProductReservation() {
        UUID productId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        insertInventory(
                productId,
                10,
                0
        );

        insertReservation(
                UUID.randomUUID(),
                orderId,
                productId,
                2,
                "RESERVED"
        );

        assertThatThrownBy(() ->
                insertReservation(
                        UUID.randomUUID(),
                        orderId,
                        productId,
                        3,
                        "RESERVED"
                )
        ).isInstanceOf(
                DataIntegrityViolationException.class
        );
    }

    @Test
    void shouldRejectReservationForUnknownInventoryItem() {
        assertThatThrownBy(() ->
                insertReservation(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        1,
                        "RESERVED"
                )
        ).isInstanceOf(
                DataIntegrityViolationException.class
        );
    }

    private void insertInventory(
            UUID productId,
            int available,
            int reserved
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO inventory_items (
                    product_id,
                    available_quantity,
                    reserved_quantity
                )
                VALUES (?, ?, ?)
                """,
                productId,
                available,
                reserved
        );
    }

    private void insertReservation(
            UUID reservationId,
            UUID orderId,
            UUID productId,
            int quantity,
            String status
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO stock_reservations (
                    id,
                    order_id,
                    product_id,
                    quantity,
                    status,
                    created_at
                )
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                reservationId,
                orderId,
                productId,
                quantity,
                status,
                now()
        );
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(
                ZoneOffset.UTC
        );
    }
}