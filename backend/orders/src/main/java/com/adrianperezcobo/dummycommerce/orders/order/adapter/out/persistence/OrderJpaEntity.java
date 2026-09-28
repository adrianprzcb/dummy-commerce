package com.adrianperezcobo.dummycommerce.orders.order.adapter.out.persistence;

import com.adrianperezcobo.dummycommerce.orders.order.domain.OrderStatus;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
public class OrderJpaEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @OneToMany(
            mappedBy = "order",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<OrderItemJpaEntity> items =
            new ArrayList<>();

    protected OrderJpaEntity() {
    }

    public OrderJpaEntity(
            UUID id,
            UUID userId,
            OrderStatus status,
            Instant createdAt
    ) {
        this.id = id;
        this.userId = userId;
        this.status = status;
        this.createdAt = createdAt;
    }

    public void addItem(
            OrderItemJpaEntity item
    ) {
        items.add(item);
        item.assignOrder(this);
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<OrderItemJpaEntity> getItems() {
        return items;
    }
}