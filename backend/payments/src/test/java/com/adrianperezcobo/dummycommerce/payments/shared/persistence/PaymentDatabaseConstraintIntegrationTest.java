package com.adrianperezcobo.dummycommerce.payments.shared.persistence;

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
class PaymentDatabaseConstraintIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer(
                    "postgres:17-alpine"
            );

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldRejectZeroAmount() {
        assertThatThrownBy(() ->
                insertPayment(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        BigDecimal.ZERO,
                        "EUR",
                        "PENDING"
                )
        ).isInstanceOf(
                DataIntegrityViolationException.class
        );
    }

    @Test
    void shouldRejectNegativeAmount() {
        assertThatThrownBy(() ->
                insertPayment(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        new BigDecimal("-1.00"),
                        "EUR",
                        "PENDING"
                )
        ).isInstanceOf(
                DataIntegrityViolationException.class
        );
    }

    @Test
    void shouldRejectInvalidCurrency() {
        assertThatThrownBy(() ->
                insertPayment(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        new BigDecimal("10.00"),
                        "eur",
                        "PENDING"
                )
        ).isInstanceOf(
                DataIntegrityViolationException.class
        );
    }

    @Test
    void shouldRejectInvalidStatus() {
        assertThatThrownBy(() ->
                insertPayment(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        new BigDecimal("10.00"),
                        "EUR",
                        "UNKNOWN"
                )
        ).isInstanceOf(
                DataIntegrityViolationException.class
        );
    }

    @Test
    void shouldRejectSecondPaymentForSameOrder() {
        UUID orderId = UUID.randomUUID();

        insertPayment(
                UUID.randomUUID(),
                orderId,
                new BigDecimal("10.00"),
                "EUR",
                "COMPLETED"
        );

        assertThatThrownBy(() ->
                insertPayment(
                        UUID.randomUUID(),
                        orderId,
                        new BigDecimal("10.00"),
                        "EUR",
                        "COMPLETED"
                )
        ).isInstanceOf(
                DataIntegrityViolationException.class
        );
    }

    private void insertPayment(
            UUID paymentId,
            UUID orderId,
            BigDecimal amount,
            String currency,
            String status
    ) {
        OffsetDateTime now =
                OffsetDateTime.now(
                        ZoneOffset.UTC
                );

        jdbcTemplate.update(
                """
                INSERT INTO payments (
                    id,
                    order_id,
                    amount,
                    currency,
                    status,
                    created_at,
                    updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                paymentId,
                orderId,
                amount,
                currency,
                status,
                now,
                now
        );
    }
}