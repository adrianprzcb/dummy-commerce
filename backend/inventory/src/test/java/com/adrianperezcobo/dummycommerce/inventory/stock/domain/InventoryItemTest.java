package com.adrianperezcobo.dummycommerce.inventory.stock.domain;

import com.adrianperezcobo.dummycommerce.inventory.stock.domain.exception.InsufficientStockException;
import com.adrianperezcobo.dummycommerce.inventory.stock.domain.exception.InvalidStockStateException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InventoryItemTest {

    @Test
    void shouldCreateInventoryItem() {
        UUID productId = UUID.randomUUID();

        InventoryItem item =
                new InventoryItem(
                        productId,
                        10,
                        2
                );

        assertThat(item.getProductId())
                .isEqualTo(productId);

        assertThat(item.getAvailableQuantity())
                .isEqualTo(10);

        assertThat(item.getReservedQuantity())
                .isEqualTo(2);

        assertThat(item.getTotalQuantity())
                .isEqualTo(12);
    }

    @Test
    void shouldIncreaseStock() {
        InventoryItem item = createItem(10, 0);

        item.increaseStock(5);

        assertThat(item.getAvailableQuantity())
                .isEqualTo(15);
    }

    @Test
    void shouldDecreaseStock() {
        InventoryItem item = createItem(10, 0);

        item.decreaseStock(4);

        assertThat(item.getAvailableQuantity())
                .isEqualTo(6);
    }

    @Test
    void shouldRejectDecreaseWhenStockIsInsufficient() {
        InventoryItem item = createItem(3, 0);

        assertThatThrownBy(() ->
                item.decreaseStock(4)
        ).isInstanceOf(
                InsufficientStockException.class
        );
    }

    @Test
    void shouldReserveStock() {
        InventoryItem item = createItem(10, 0);

        item.reserve(3);

        assertThat(item.getAvailableQuantity())
                .isEqualTo(7);

        assertThat(item.getReservedQuantity())
                .isEqualTo(3);

        assertThat(item.getTotalQuantity())
                .isEqualTo(10);
    }

    @Test
    void shouldRejectReservationWhenStockIsInsufficient() {
        InventoryItem item = createItem(2, 0);

        assertThatThrownBy(() ->
                item.reserve(3)
        ).isInstanceOf(
                InsufficientStockException.class
        );
    }

    @Test
    void shouldReleaseReservedStock() {
        InventoryItem item = createItem(7, 3);

        item.release(3);

        assertThat(item.getAvailableQuantity())
                .isEqualTo(10);

        assertThat(item.getReservedQuantity())
                .isZero();
    }

    @Test
    void shouldRejectReleasingMoreThanReserved() {
        InventoryItem item = createItem(7, 3);

        assertThatThrownBy(() ->
                item.release(4)
        ).isInstanceOf(
                InvalidStockStateException.class
        );
    }

    @Test
    void shouldConfirmReservation() {
        InventoryItem item = createItem(7, 3);

        item.confirmReservation(3);

        assertThat(item.getAvailableQuantity())
                .isEqualTo(7);

        assertThat(item.getReservedQuantity())
                .isZero();

        assertThat(item.getTotalQuantity())
                .isEqualTo(7);
    }

    @Test
    void shouldRejectNonPositiveQuantities() {
        InventoryItem item = createItem(10, 0);

        assertThatThrownBy(() ->
                item.reserve(0)
        ).isInstanceOf(
                IllegalArgumentException.class
        );

        assertThatThrownBy(() ->
                item.increaseStock(-1)
        ).isInstanceOf(
                IllegalArgumentException.class
        );
    }

    @Test
    void shouldRejectNegativeInitialQuantities() {
        UUID productId = UUID.randomUUID();

        assertThatThrownBy(() ->
                new InventoryItem(
                        productId,
                        -1,
                        0
                )
        ).isInstanceOf(
                IllegalArgumentException.class
        );

        assertThatThrownBy(() ->
                new InventoryItem(
                        productId,
                        0,
                        -1
                )
        ).isInstanceOf(
                IllegalArgumentException.class
        );
    }

    private InventoryItem createItem(
            int available,
            int reserved
    ) {
        return new InventoryItem(
                UUID.randomUUID(),
                available,
                reserved
        );
    }
}