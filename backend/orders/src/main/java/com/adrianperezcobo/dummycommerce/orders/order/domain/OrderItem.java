package com.adrianperezcobo.dummycommerce.orders.order.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.UUID;

public class OrderItem {

    private final UUID id;
    private final UUID productId;
    private final int quantity;
    private final BigDecimal unitPrice;

    public OrderItem(
            UUID id,
            UUID productId,
            int quantity,
            BigDecimal unitPrice
    ) {
        this.id = Objects.requireNonNull(id);
        this.productId = Objects.requireNonNull(productId);

        if (quantity <= 0) {
            throw new IllegalArgumentException(
                    "Quantity must be greater than zero"
            );
        }

        if (unitPrice == null) {
            throw new IllegalArgumentException(
                    "Unit price is required"
            );
        }

        if (unitPrice.signum() < 0) {
            throw new IllegalArgumentException(
                    "Unit price cannot be negative"
            );
        }

        this.quantity = quantity;

        this.unitPrice = unitPrice.setScale(
                2,
                RoundingMode.HALF_UP
        );
    }

    public BigDecimal getSubtotal() {
        return unitPrice
                .multiply(
                        BigDecimal.valueOf(quantity)
                )
                .setScale(
                        2,
                        RoundingMode.HALF_UP
                );
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