package com.adrianperezcobo.dummycommerce.inventory.stock.domain;

import com.adrianperezcobo.dummycommerce.inventory.stock.domain.exception.InsufficientStockException;
import com.adrianperezcobo.dummycommerce.inventory.stock.domain.exception.InvalidStockStateException;

import java.util.Objects;
import java.util.UUID;

public class InventoryItem {

    private final UUID productId;

    private int availableQuantity;
    private int reservedQuantity;

    public InventoryItem(
            UUID productId,
            int availableQuantity,
            int reservedQuantity
    ) {
        this.productId = Objects.requireNonNull(productId);

        if (availableQuantity < 0) {
            throw new IllegalArgumentException(
                    "Available quantity cannot be negative"
            );
        }

        if (reservedQuantity < 0) {
            throw new IllegalArgumentException(
                    "Reserved quantity cannot be negative"
            );
        }

        this.availableQuantity = availableQuantity;
        this.reservedQuantity = reservedQuantity;
    }

    public void increaseStock(int quantity) {
        validatePositiveQuantity(quantity);

        availableQuantity += quantity;
    }

    public void decreaseStock(int quantity) {
        validatePositiveQuantity(quantity);

        if (availableQuantity < quantity) {
            throw new InsufficientStockException(
                    productId,
                    quantity,
                    availableQuantity
            );
        }

        availableQuantity -= quantity;
    }

    public void reserve(int quantity) {
        validatePositiveQuantity(quantity);

        if (availableQuantity < quantity) {
            throw new InsufficientStockException(
                    productId,
                    quantity,
                    availableQuantity
            );
        }

        availableQuantity -= quantity;
        reservedQuantity += quantity;
    }

    public void release(int quantity) {
        validatePositiveQuantity(quantity);

        if (reservedQuantity < quantity) {
            throw new InvalidStockStateException(
                    "Cannot release more stock than currently reserved"
            );
        }

        reservedQuantity -= quantity;
        availableQuantity += quantity;
    }

    public void confirmReservation(int quantity) {
        validatePositiveQuantity(quantity);

        if (reservedQuantity < quantity) {
            throw new InvalidStockStateException(
                    "Cannot confirm more stock than currently reserved"
            );
        }

        reservedQuantity -= quantity;
    }

    private void validatePositiveQuantity(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException(
                    "Quantity must be greater than zero"
            );
        }
    }

    public UUID getProductId() {
        return productId;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }

    public int getReservedQuantity() {
        return reservedQuantity;
    }

    public int getTotalQuantity() {
        return availableQuantity + reservedQuantity;
    }
}