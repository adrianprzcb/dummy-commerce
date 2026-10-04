package com.adrianperezcobo.dummycommerce.payments.payment.adapter.out.persistence;

import com.adrianperezcobo.dummycommerce.payments.payment.application.port.out.PaymentRepository;
import com.adrianperezcobo.dummycommerce.payments.payment.domain.Payment;
import com.adrianperezcobo.dummycommerce.payments.payment.domain.PaymentStatus;
import jakarta.persistence.EntityManager;
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
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.NONE;

@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@Import(PaymentPersistenceAdapter.class)
@Testcontainers
class PaymentPersistenceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer(
                    "postgres:17-alpine"
            );

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldPersistAndLoadPayment() {
        Payment payment =
                createPayment(
                        PaymentStatus.PENDING
                );

        paymentRepository.save(payment);

        flushAndClear();

        Payment loaded =
                paymentRepository
                        .findById(payment.getId())
                        .orElseThrow();

        assertThat(loaded.getOrderId())
                .isEqualTo(payment.getOrderId());

        assertThat(loaded.getAmount())
                .isEqualByComparingTo("49.98");

        assertThat(loaded.getCurrency())
                .isEqualTo("EUR");

        assertThat(loaded.getStatus())
                .isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    void shouldFindPaymentByOrderId() {
        Payment payment =
                createPayment(
                        PaymentStatus.COMPLETED
                );

        paymentRepository.save(payment);

        flushAndClear();

        Payment loaded =
                paymentRepository
                        .findByOrderId(
                                payment.getOrderId()
                        )
                        .orElseThrow();

        assertThat(loaded.getId())
                .isEqualTo(payment.getId());

        assertThat(
                paymentRepository.existsByOrderId(
                        payment.getOrderId()
                )
        ).isTrue();
    }

    @Test
    void shouldPersistStatusChange() {
        Payment payment =
                createPayment(
                        PaymentStatus.PENDING
                );

        paymentRepository.save(payment);

        flushAndClear();

        Payment loaded =
                paymentRepository
                        .findByIdForUpdate(
                                payment.getId()
                        )
                        .orElseThrow();

        loaded.complete(Instant.now());

        paymentRepository.save(loaded);

        flushAndClear();

        Payment updated =
                paymentRepository
                        .findById(
                                payment.getId()
                        )
                        .orElseThrow();

        assertThat(updated.getStatus())
                .isEqualTo(PaymentStatus.COMPLETED);
    }

    @Test
    void shouldLoadPaymentByOrderIdForUpdate() {
        Payment payment =
                createPayment(
                        PaymentStatus.PENDING
                );

        paymentRepository.save(payment);

        flushAndClear();

        Payment loaded =
                paymentRepository
                        .findByOrderIdForUpdate(
                                payment.getOrderId()
                        )
                        .orElseThrow();

        assertThat(loaded.getId())
                .isEqualTo(payment.getId());
    }

    private Payment createPayment(
            PaymentStatus status
    ) {
        Instant now = Instant.now();

        return new Payment(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("49.98"),
                "EUR",
                status,
                now,
                now
        );
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}