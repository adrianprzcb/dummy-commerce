package com.adrianperezcobo.dummycommerce.inventory.reservation.adapter.out.persistence;

import com.adrianperezcobo.dummycommerce.inventory.reservation.domain.ReservationStatus;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "stock_reservations",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_stock_reservation_order_product",
                columnNames = {
                        "order_id",
                        "product_id"
                }
        )
)
public class StockReservationJpaEntity {

    @Id
    private UUID id;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(nullable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReservationStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected StockReservationJpaEntity() {
    }

    public StockReservationJpaEntity(
            UUID id,
            UUID orderId,
            UUID productId,
            int quantity,
            ReservationStatus status,
            Instant createdAt
    ) {
        this.id = id;
        this.orderId = orderId;
        this.productId = productId;
        this.quantity = quantity;
        this.status = status;
        this.createdAt = createdAt;
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