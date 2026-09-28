package com.adrianperezcobo.dummycommerce.orders.order.adapter.out.persistence;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(
        name = "order_items",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_order_items_order_product",
                columnNames = {
                        "order_id",
                        "product_id"
                }
        )
)
public class OrderItemJpaEntity {

    @Id
    private UUID id;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "order_id",
            nullable = false
    )
    private OrderJpaEntity order;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(nullable = false)
    private int quantity;

    @Column(
            name = "unit_price",
            nullable = false,
            precision = 19,
            scale = 2
    )
    private BigDecimal unitPrice;

    protected OrderItemJpaEntity() {
    }

    public OrderItemJpaEntity(
            UUID id,
            UUID productId,
            int quantity,
            BigDecimal unitPrice
    ) {
        this.id = id;
        this.productId = productId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    void assignOrder(
            OrderJpaEntity order
    ) {
        this.order = order;
    }

    public UUID getId() {
        return id;
    }

    public UUID getProductId() {
        return productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }
}