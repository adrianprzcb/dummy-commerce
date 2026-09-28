package com.adrianperezcobo.dummycommerce.orders.order.domain;

import com.adrianperezcobo.dummycommerce.orders.order.domain.exception.InvalidOrderStateException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Order {

    private final UUID id;
    private final UUID userId;
    private final List<OrderItem> items;
    private final Instant createdAt;

    private OrderStatus status;

    public Order(
            UUID id,
            UUID userId,
            List<OrderItem> items,
            OrderStatus status,
            Instant createdAt
    ) {
        this.id = Objects.requireNonNull(id);
        this.userId = Objects.requireNonNull(userId);
        this.status = Objects.requireNonNull(status);
        this.createdAt = Objects.requireNonNull(createdAt);

        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException(
                    "Order must contain at least one item"
            );
        }

        ensureNoDuplicatedProducts(items);

        this.items = new ArrayList<>(items);
    }

    public void markStockReserved() {
        ensureStatus(OrderStatus.CREATED);

        status = OrderStatus.STOCK_RESERVED;
    }

    public void markPaymentCompleted() {
        ensureStatus(OrderStatus.STOCK_RESERVED);

        status = OrderStatus.PAYMENT_COMPLETED;
    }

    public void confirm() {
        ensureStatus(OrderStatus.PAYMENT_COMPLETED);

        status = OrderStatus.CONFIRMED;
    }

    public void cancel() {
        if (status == OrderStatus.CONFIRMED) {
            throw new InvalidOrderStateException(
                    "Confirmed order cannot be cancelled"
            );
        }

        if (status == OrderStatus.CANCELLED) {
            throw new InvalidOrderStateException(
                    "Order is already cancelled"
            );
        }

        status = OrderStatus.CANCELLED;
    }

    public BigDecimal getTotalAmount() {
        return items.stream()
                .map(OrderItem::getSubtotal)
                .reduce(
                        BigDecimal.ZERO,
                        BigDecimal::add
                )
                .setScale(
                        2,
                        RoundingMode.HALF_UP
                );
    }

    private void ensureStatus(
            OrderStatus expected
    ) {
        if (status != expected) {
            throw new InvalidOrderStateException(
                    "Order must be " +
                            expected +
                            " but is " +
                            status
            );
        }
    }

    private void ensureNoDuplicatedProducts(
            List<OrderItem> items
    ) {
        long distinctProducts =
                items.stream()
                        .map(OrderItem::getProductId)
                        .distinct()
                        .count();

        if (distinctProducts != items.size()) {
            throw new IllegalArgumentException(
                    "Order cannot contain duplicated products"
            );
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(
                items
        );
    }

    public OrderStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}