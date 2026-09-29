package com.adrianperezcobo.dummycommerce.orders.shared.persistence;

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

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.NONE;

@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@Testcontainers
class OrderDatabaseConstraintIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer(
                    "postgres:17-alpine"
            );

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldRejectInvalidOrderStatus() {
        assertThatThrownBy(() ->
                insertOrder(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "INVALID_STATUS"
                )
        ).isInstanceOf(
                DataIntegrityViolationException.class
        );
    }

    @Test
    void shouldRejectNonPositiveItemQuantity() {
        UUID orderId = UUID.randomUUID();

        insertValidOrder(orderId);

        assertThatThrownBy(() ->
                insertItem(
                        UUID.randomUUID(),
                        orderId,
                        UUID.randomUUID(),
                        0,
                        new BigDecimal("10.00")
                )
        ).isInstanceOf(
                DataIntegrityViolationException.class
        );
    }

    @Test
    void shouldRejectNegativeUnitPrice() {
        UUID orderId = UUID.randomUUID();

        insertValidOrder(orderId);

        assertThatThrownBy(() ->
                insertItem(
                        UUID.randomUUID(),
                        orderId,
                        UUID.randomUUID(),
                        1,
                        new BigDecimal("-0.01")
                )
        ).isInstanceOf(
                DataIntegrityViolationException.class
        );
    }

    @Test
    void shouldAllowZeroUnitPrice() {
        UUID orderId = UUID.randomUUID();

        insertValidOrder(orderId);

        insertItem(
                UUID.randomUUID(),
                orderId,
                UUID.randomUUID(),
                1,
                BigDecimal.ZERO
        );
    }

    @Test
    void shouldRejectDuplicatedProductInSameOrder() {
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        insertValidOrder(orderId);

        insertItem(
                UUID.randomUUID(),
                orderId,
                productId,
                1,
                new BigDecimal("10.00")
        );

        assertThatThrownBy(() ->
                insertItem(
                        UUID.randomUUID(),
                        orderId,
                        productId,
                        2,
                        new BigDecimal("10.00")
                )
        ).isInstanceOf(
                DataIntegrityViolationException.class
        );
    }

    @Test
    void shouldAllowSameProductInDifferentOrders() {
        UUID firstOrderId = UUID.randomUUID();
        UUID secondOrderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        insertValidOrder(firstOrderId);
        insertValidOrder(secondOrderId);

        insertItem(
                UUID.randomUUID(),
                firstOrderId,
                productId,
                1,
                new BigDecimal("10.00")
        );

        insertItem(
                UUID.randomUUID(),
                secondOrderId,
                productId,
                1,
                new BigDecimal("10.00")
        );
    }

    @Test
    void shouldRejectItemForUnknownOrder() {
        assertThatThrownBy(() ->
                insertItem(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        1,
                        new BigDecimal("10.00")
                )
        ).isInstanceOf(
                DataIntegrityViolationException.class
        );
    }

    private void insertValidOrder(
            UUID orderId
    ) {
        insertOrder(
                orderId,
                UUID.randomUUID(),
                "CREATED"
        );
    }

    private void insertOrder(
            UUID orderId,
            UUID userId,
            String status
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO orders (
                    id,
                    user_id,
                    status,
                    created_at
                )
                VALUES (?, ?, ?, ?)
                """,
                orderId,
                userId,
                status,
                now()
        );
    }

    private void insertItem(
            UUID itemId,
            UUID orderId,
            UUID productId,
            int quantity,
            BigDecimal unitPrice
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO order_items (
                    id,
                    order_id,
                    product_id,
                    quantity,
                    unit_price
                )
                VALUES (?, ?, ?, ?, ?)
                """,
                itemId,
                orderId,
                productId,
                quantity,
                unitPrice
        );
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(
                ZoneOffset.UTC
        );
    }
}