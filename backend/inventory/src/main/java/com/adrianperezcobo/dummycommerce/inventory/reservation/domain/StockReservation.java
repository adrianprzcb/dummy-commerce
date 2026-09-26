package com.adrianperezcobo.dummycommerce.inventory.reservation.domain;

import com.adrianperezcobo.dummycommerce.inventory.reservation.domain.exception.InvalidReservationStateException;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class StockReservation {

    private final UUID id;
    private final UUID orderId;
    private final UUID productId;
    private final int quantity;
    private final Instant createdAt;

    private ReservationStatus status;

    public StockReservation(
            UUID id,
            UUID orderId,
            UUID productId,
            int quantity,
            ReservationStatus status,
            Instant createdAt
    ) {
        this.id = Objects.requireNonNull(id);
        this.orderId = Objects.requireNonNull(orderId);
        this.productId = Objects.requireNonNull(productId);
        this.status = Objects.requireNonNull(status);
        this.createdAt = Objects.requireNonNull(createdAt);

        if (quantity <= 0) {
            throw new IllegalArgumentException(
                    "Reservation quantity must be greater than zero"
            );
        }

        this.quantity = quantity;
    }

    public void confirm() {
        ensureReserved();

        status = ReservationStatus.CONFIRMED;
    }

    public void release() {
        ensureReserved();

        status = ReservationStatus.RELEASED;
    }

    private void ensureReserved() {
        if (status != ReservationStatus.RESERVED) {
            throw new InvalidReservationStateException(
                    "Reservation must be RESERVED"
            );
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public UUID getProductId() {
        return productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}