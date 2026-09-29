package com.adrianperezcobo.dummycommerce.orders.shared.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.NONE;

@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@Testcontainers
class OrderCascadeIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer(
                    "postgres:17-alpine"
            );

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldDeleteOrderItemsWhenOrderIsDeleted() {
        UUID orderId = UUID.randomUUID();

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
                UUID.randomUUID(),
                "CREATED",
                OffsetDateTime.now(
                        ZoneOffset.UTC
                )
        );

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
                UUID.randomUUID(),
                orderId,
                UUID.randomUUID(),
                2,
                new BigDecimal("19.99")
        );

        jdbcTemplate.update(
                """
                DELETE FROM orders
                WHERE id = ?
                """,
                orderId
        );

        Long items =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM order_items
                        WHERE order_id = ?
                        """,
                        Long.class,
                        orderId
                );

        assertThat(items)
                .isZero();
    }
}