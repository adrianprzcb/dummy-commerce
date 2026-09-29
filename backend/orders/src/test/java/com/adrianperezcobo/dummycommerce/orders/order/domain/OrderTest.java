package com.adrianperezcobo.dummycommerce.orders.order.domain;

import com.adrianperezcobo.dummycommerce.orders.order.domain.exception.InvalidOrderStateException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTest {

    @Test
    void shouldCreateOrder() {
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Order order = new Order(
                orderId,
                userId,
                List.of(
                        item(
                                UUID.randomUUID(),
                                2,
                                "10.00"
                        )
                ),
                OrderStatus.CREATED,
                Instant.now()
        );

        assertThat(order.getId())
                .isEqualTo(orderId);

        assertThat(order.getUserId())
                .isEqualTo(userId);

        assertThat(order.getStatus())
                .isEqualTo(OrderStatus.CREATED);

        assertThat(order.getItems())
                .hasSize(1);
    }

    @Test
    void shouldCalculateTotalAmount() {
        Order order = createOrder(
                OrderStatus.CREATED,
                List.of(
                        item(
                                UUID.randomUUID(),
                                2,
                                "19.99"
                        ),
                        item(
                                UUID.randomUUID(),
                                3,
                                "5.50"
                        )
                )
        );

        // 39.98 + 16.50
        assertThat(order.getTotalAmount())
                .isEqualByComparingTo("56.48");
    }

    @Test
    void shouldRejectEmptyOrder() {
        assertThatThrownBy(() ->
                createOrder(
                        OrderStatus.CREATED,
                        List.of()
                )
        ).isInstanceOf(
                IllegalArgumentException.class
        );
    }

    @Test
    void shouldRejectDuplicatedProducts() {
        UUID productId = UUID.randomUUID();

        assertThatThrownBy(() ->
                createOrder(
                        OrderStatus.CREATED,
                        List.of(
                                item(
                                        productId,
                                        1,
                                        "10.00"
                                ),
                                item(
                                        productId,
                                        2,
                                        "10.00"
                                )
                        )
                )
        ).isInstanceOf(
                IllegalArgumentException.class
        );
    }

    @Test
    void shouldFollowCompleteOrderFlow() {
        Order order = createOrder(
                OrderStatus.CREATED
        );

        order.markStockReserved();

        assertThat(order.getStatus())
                .isEqualTo(
                        OrderStatus.STOCK_RESERVED
                );

        order.markPaymentCompleted();

        assertThat(order.getStatus())
                .isEqualTo(
                        OrderStatus.PAYMENT_COMPLETED
                );

        order.confirm();

        assertThat(order.getStatus())
                .isEqualTo(
                        OrderStatus.CONFIRMED
                );
    }

    @Test
    void shouldRejectStockReservedFromWrongState() {
        Order order = createOrder(
                OrderStatus.STOCK_RESERVED
        );

        assertThatThrownBy(
                order::markStockReserved
        ).isInstanceOf(
                InvalidOrderStateException.class
        );
    }

    @Test
    void shouldRejectPaymentCompletedBeforeStockReservation() {
        Order order = createOrder(
                OrderStatus.CREATED
        );

        assertThatThrownBy(
                order::markPaymentCompleted
        ).isInstanceOf(
                InvalidOrderStateException.class
        );
    }

    @Test
    void shouldRejectConfirmationBeforePayment() {
        Order order = createOrder(
                OrderStatus.STOCK_RESERVED
        );

        assertThatThrownBy(
                order::confirm
        ).isInstanceOf(
                InvalidOrderStateException.class
        );
    }

    @Test
    void shouldCancelCreatedOrder() {
        Order order = createOrder(
                OrderStatus.CREATED
        );

        order.cancel();

        assertThat(order.getStatus())
                .isEqualTo(
                        OrderStatus.CANCELLED
                );
    }

    @Test
    void shouldCancelStockReservedOrder() {
        Order order = createOrder(
                OrderStatus.STOCK_RESERVED
        );

        order.cancel();

        assertThat(order.getStatus())
                .isEqualTo(
                        OrderStatus.CANCELLED
                );
    }

    @Test
    void shouldCancelPaymentCompletedOrder() {
        Order order = createOrder(
                OrderStatus.PAYMENT_COMPLETED
        );

        order.cancel();

        assertThat(order.getStatus())
                .isEqualTo(
                        OrderStatus.CANCELLED
                );
    }

    @Test
    void shouldRejectCancellingConfirmedOrder() {
        Order order = createOrder(
                OrderStatus.CONFIRMED
        );

        assertThatThrownBy(
                order::cancel
        ).isInstanceOf(
                InvalidOrderStateException.class
        );
    }

    @Test
    void shouldRejectCancellingAlreadyCancelledOrder() {
        Order order = createOrder(
                OrderStatus.CANCELLED
        );

        assertThatThrownBy(
                order::cancel
        ).isInstanceOf(
                InvalidOrderStateException.class
        );
    }

    @Test
    void shouldExposeUnmodifiableItems() {
        Order order = createOrder(
                OrderStatus.CREATED
        );

        assertThatThrownBy(() ->
                order.getItems().add(
                        item(
                                UUID.randomUUID(),
                                1,
                                "1.00"
                        )
                )
        ).isInstanceOf(
                UnsupportedOperationException.class
        );
    }

    private Order createOrder(
            OrderStatus status
    ) {
        return createOrder(
                status,
                List.of(
                        item(
                                UUID.randomUUID(),
                                1,
                                "10.00"
                        )
                )
        );
    }

    private Order createOrder(
            OrderStatus status,
            List<OrderItem> items
    ) {
        return new Order(
                UUID.randomUUID(),
                UUID.randomUUID(),
                items,
                status,
                Instant.now()
        );
    }

    private OrderItem item(
            UUID productId,
            int quantity,
            String unitPrice
    ) {
        return new OrderItem(
                UUID.randomUUID(),
                productId,
                quantity,
                new BigDecimal(unitPrice)
        );
    }
}