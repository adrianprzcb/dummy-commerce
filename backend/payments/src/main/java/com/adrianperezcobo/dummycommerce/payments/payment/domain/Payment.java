package com.adrianperezcobo.dummycommerce.payments.payment.domain;

import com.adrianperezcobo.dummycommerce.payments.payment.domain.exception.InvalidPaymentStateException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public class Payment {

    private final UUID id;
    private final UUID orderId;
    private final BigDecimal amount;
    private final String currency;
    private final Instant createdAt;

    private PaymentStatus status;
    private Instant updatedAt;

    public Payment(
            UUID id,
            UUID orderId,
            BigDecimal amount,
            String currency,
            PaymentStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id);
        this.orderId = Objects.requireNonNull(orderId);
        this.status = Objects.requireNonNull(status);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);

        if (amount == null) {
            throw new IllegalArgumentException(
                    "Payment amount is required"
            );
        }

        BigDecimal normalizedAmount =
                amount.setScale(
                        2,
                        RoundingMode.HALF_UP
                );

        if (normalizedAmount.signum() <= 0) {
            throw new IllegalArgumentException(
                    "Payment amount must be greater than zero"
            );
        }

        if (currency == null ||
                !currency.matches("^[A-Za-z]{3}$")) {

            throw new IllegalArgumentException(
                    "Currency must be a 3-letter code"
            );
        }

        this.amount = normalizedAmount;

        this.currency = currency
                .toUpperCase(Locale.ROOT);
    }

    public void complete(
            Instant now
    ) {
        ensureStatus(PaymentStatus.PENDING);

        status = PaymentStatus.COMPLETED;
        updatedAt = Objects.requireNonNull(now);
    }

    public void fail(
            Instant now
    ) {
        ensureStatus(PaymentStatus.PENDING);

        status = PaymentStatus.FAILED;
        updatedAt = Objects.requireNonNull(now);
    }

    public void refund(
            Instant now
    ) {
        ensureStatus(PaymentStatus.COMPLETED);

        status = PaymentStatus.REFUNDED;
        updatedAt = Objects.requireNonNull(now);
    }

    private void ensureStatus(
            PaymentStatus expected
    ) {
        if (status != expected) {
            throw new InvalidPaymentStateException(
                    "Payment must be " +
                            expected +
                            " but is " +
                            status
            );
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}