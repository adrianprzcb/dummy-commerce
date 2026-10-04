package com.adrianperezcobo.dummycommerce.payments.payment.domain;

import com.adrianperezcobo.dummycommerce.payments.payment.domain.exception.InvalidPaymentStateException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentTest {

    @Test
    void shouldCreatePaymentAndNormalizeValues() {
        Payment payment = new Payment(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("19.999"),
                "eur",
                PaymentStatus.PENDING,
                Instant.now(),
                Instant.now()
        );

        assertThat(payment.getAmount())
                .isEqualByComparingTo("20.00");

        assertThat(payment.getCurrency())
                .isEqualTo("EUR");

        assertThat(payment.getStatus())
                .isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    void shouldCompletePendingPayment() {
        Payment payment =
                payment(PaymentStatus.PENDING);

        Instant completedAt = Instant.now();

        payment.complete(completedAt);

        assertThat(payment.getStatus())
                .isEqualTo(PaymentStatus.COMPLETED);

        assertThat(payment.getUpdatedAt())
                .isEqualTo(completedAt);
    }

    @Test
    void shouldFailPendingPayment() {
        Payment payment =
                payment(PaymentStatus.PENDING);

        payment.fail(Instant.now());

        assertThat(payment.getStatus())
                .isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    void shouldRefundCompletedPayment() {
        Payment payment =
                payment(PaymentStatus.COMPLETED);

        payment.refund(Instant.now());

        assertThat(payment.getStatus())
                .isEqualTo(PaymentStatus.REFUNDED);
    }

    @Test
    void shouldRejectCompletingCompletedPayment() {
        Payment payment =
                payment(PaymentStatus.COMPLETED);

        assertThatThrownBy(() ->
                payment.complete(Instant.now())
        ).isInstanceOf(
                InvalidPaymentStateException.class
        );
    }

    @Test
    void shouldRejectFailingCompletedPayment() {
        Payment payment =
                payment(PaymentStatus.COMPLETED);

        assertThatThrownBy(() ->
                payment.fail(Instant.now())
        ).isInstanceOf(
                InvalidPaymentStateException.class
        );
    }

    @Test
    void shouldRejectRefundingPendingPayment() {
        Payment payment =
                payment(PaymentStatus.PENDING);

        assertThatThrownBy(() ->
                payment.refund(Instant.now())
        ).isInstanceOf(
                InvalidPaymentStateException.class
        );
    }

    @Test
    void shouldRejectRefundingFailedPayment() {
        Payment payment =
                payment(PaymentStatus.FAILED);

        assertThatThrownBy(() ->
                payment.refund(Instant.now())
        ).isInstanceOf(
                InvalidPaymentStateException.class
        );
    }

    @Test
    void shouldRejectZeroAmount() {
        assertThatThrownBy(() ->
                createWithAmount(
                        BigDecimal.ZERO
                )
        ).isInstanceOf(
                IllegalArgumentException.class
        );
    }

    @Test
    void shouldRejectNegativeAmount() {
        assertThatThrownBy(() ->
                createWithAmount(
                        new BigDecimal("-0.01")
                )
        ).isInstanceOf(
                IllegalArgumentException.class
        );
    }

    @Test
    void shouldRejectAmountThatRoundsToZero() {
        assertThatThrownBy(() ->
                createWithAmount(
                        new BigDecimal("0.001")
                )
        ).isInstanceOf(
                IllegalArgumentException.class
        );
    }

    @Test
    void shouldRejectInvalidCurrency() {
        assertThatThrownBy(() ->
                new Payment(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        new BigDecimal("10.00"),
                        "EU",
                        PaymentStatus.PENDING,
                        Instant.now(),
                        Instant.now()
                )
        ).isInstanceOf(
                IllegalArgumentException.class
        );
    }

    private Payment payment(
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

    private void createWithAmount(
            BigDecimal amount
    ) {
        Instant now = Instant.now();

        new Payment(
                UUID.randomUUID(),
                UUID.randomUUID(),
                amount,
                "EUR",
                PaymentStatus.PENDING,
                now,
                now
        );
    }
}