package com.adrianperezcobo.dummycommerce.orders.order.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderItemTest {

    @Test
    void shouldCreateOrderItem() {
        UUID id = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        OrderItem item = new OrderItem(
                id,
                productId,
                2,
                new BigDecimal("19.99")
        );

        assertThat(item.getId())
                .isEqualTo(id);

        assertThat(item.getProductId())
                .isEqualTo(productId);

        assertThat(item.getQuantity())
                .isEqualTo(2);

        assertThat(item.getUnitPrice())
                .isEqualByComparingTo("19.99");
    }

    @Test
    void shouldCalculateSubtotal() {
        OrderItem item = new OrderItem(
                UUID.randomUUID(),
                UUID.randomUUID(),
                3,
                new BigDecimal("19.99")
        );

        assertThat(item.getSubtotal())
                .isEqualByComparingTo("59.97");
    }

    @Test
    void shouldRoundUnitPriceToTwoDecimals() {
        OrderItem item = new OrderItem(
                UUID.randomUUID(),
                UUID.randomUUID(),
                1,
                new BigDecimal("19.995")
        );

        assertThat(item.getUnitPrice())
                .isEqualByComparingTo("20.00");
    }

    @Test
    void shouldAllowZeroPrice() {
        OrderItem item = new OrderItem(
                UUID.randomUUID(),
                UUID.randomUUID(),
                1,
                BigDecimal.ZERO
        );

        assertThat(item.getSubtotal())
                .isEqualByComparingTo("0.00");
    }

    @Test
    void shouldRejectNonPositiveQuantity() {
        assertThatThrownBy(() ->
                new OrderItem(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        0,
                        new BigDecimal("10.00")
                )
        ).isInstanceOf(
                IllegalArgumentException.class
        );
    }

    @Test
    void shouldRejectNegativePrice() {
        assertThatThrownBy(() ->
                new OrderItem(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        1,
                        new BigDecimal("-0.01")
                )
        ).isInstanceOf(
                IllegalArgumentException.class
        );
    }

    @Test
    void shouldRejectNullPrice() {
        assertThatThrownBy(() ->
                new OrderItem(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        1,
                        null
                )
        ).isInstanceOf(
                IllegalArgumentException.class
        );
    }
}