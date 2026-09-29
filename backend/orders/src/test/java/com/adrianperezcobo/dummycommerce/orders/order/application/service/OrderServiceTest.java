package com.adrianperezcobo.dummycommerce.orders.order.application.service;

import com.adrianperezcobo.dummycommerce.orders.order.application.command.CreateOrderCommand;
import com.adrianperezcobo.dummycommerce.orders.order.application.command.CreateOrderItemCommand;
import com.adrianperezcobo.dummycommerce.orders.order.application.exception.OrderNotFoundException;
import com.adrianperezcobo.dummycommerce.orders.order.application.port.out.OrderRepository;
import com.adrianperezcobo.dummycommerce.orders.order.domain.Order;
import com.adrianperezcobo.dummycommerce.orders.order.domain.OrderItem;
import com.adrianperezcobo.dummycommerce.orders.order.domain.OrderStatus;
import com.adrianperezcobo.dummycommerce.orders.order.domain.exception.InvalidOrderStateException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderService service;

    @Test
    void shouldCreateOrder() {
        UUID userId = UUID.randomUUID();
        UUID productA = UUID.randomUUID();
        UUID productB = UUID.randomUUID();

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(
                        invocation ->
                                invocation.getArgument(0)
                );

        Order result = service.create(
                new CreateOrderCommand(
                        userId,
                        List.of(
                                new CreateOrderItemCommand(
                                        productA,
                                        2,
                                        new BigDecimal("19.99")
                                ),
                                new CreateOrderItemCommand(
                                        productB,
                                        3,
                                        new BigDecimal("5.50")
                                )
                        )
                )
        );

        assertThat(result.getId())
                .isNotNull();

        assertThat(result.getUserId())
                .isEqualTo(userId);

        assertThat(result.getStatus())
                .isEqualTo(OrderStatus.CREATED);

        assertThat(result.getItems())
                .hasSize(2);

        assertThat(result.getTotalAmount())
                .isEqualByComparingTo("56.48");

        verify(orderRepository)
                .save(any(Order.class));
    }

    @Test
    void shouldRejectDuplicatedProductsWhenCreatingOrder() {
        UUID productId = UUID.randomUUID();

        CreateOrderCommand command =
                new CreateOrderCommand(
                        UUID.randomUUID(),
                        List.of(
                                new CreateOrderItemCommand(
                                        productId,
                                        1,
                                        new BigDecimal("10.00")
                                ),
                                new CreateOrderItemCommand(
                                        productId,
                                        2,
                                        new BigDecimal("10.00")
                                )
                        )
                );

        assertThatThrownBy(() ->
                service.create(command)
        ).isInstanceOf(
                IllegalArgumentException.class
        );

        verify(orderRepository, never())
                .save(any());
    }

    @Test
    void shouldGetOrderById() {
        Order order = createOrder(
                OrderStatus.CREATED
        );

        when(orderRepository.findById(
                order.getId()
        )).thenReturn(Optional.of(order));

        Order result =
                service.getById(order.getId());

        assertThat(result)
                .isSameAs(order);
    }

    @Test
    void shouldThrowWhenOrderDoesNotExist() {
        UUID orderId = UUID.randomUUID();

        when(orderRepository.findById(orderId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.getById(orderId)
        ).isInstanceOf(
                OrderNotFoundException.class
        );
    }

    @Test
    void shouldGetOrdersByUser() {
        UUID userId = UUID.randomUUID();

        Order first =
                createOrder(
                        userId,
                        OrderStatus.CREATED
                );

        Order second =
                createOrder(
                        userId,
                        OrderStatus.CONFIRMED
                );

        when(orderRepository.findByUserId(userId))
                .thenReturn(
                        List.of(first, second)
                );

        List<Order> result =
                service.getByUserId(userId);

        assertThat(result)
                .containsExactly(
                        first,
                        second
                );
    }

    @Test
    void shouldMarkStockReservedUsingLockedRead() {
        Order order = createOrder(
                OrderStatus.CREATED
        );

        mockLockedOrder(order);

        Order result =
                service.markStockReserved(
                        order.getId()
                );

        assertThat(result.getStatus())
                .isEqualTo(
                        OrderStatus.STOCK_RESERVED
                );

        verifyLockedRead(order.getId());
        verify(orderRepository).save(order);
    }

    @Test
    void shouldMarkPaymentCompletedUsingLockedRead() {
        Order order = createOrder(
                OrderStatus.STOCK_RESERVED
        );

        mockLockedOrder(order);

        Order result =
                service.markPaymentCompleted(
                        order.getId()
                );

        assertThat(result.getStatus())
                .isEqualTo(
                        OrderStatus.PAYMENT_COMPLETED
                );

        verifyLockedRead(order.getId());
        verify(orderRepository).save(order);
    }

    @Test
    void shouldConfirmOrderUsingLockedRead() {
        Order order = createOrder(
                OrderStatus.PAYMENT_COMPLETED
        );

        mockLockedOrder(order);

        Order result =
                service.confirm(
                        order.getId()
                );

        assertThat(result.getStatus())
                .isEqualTo(
                        OrderStatus.CONFIRMED
                );

        verifyLockedRead(order.getId());
        verify(orderRepository).save(order);
    }

    @Test
    void shouldCancelOrderUsingLockedRead() {
        Order order = createOrder(
                OrderStatus.STOCK_RESERVED
        );

        mockLockedOrder(order);

        Order result =
                service.cancel(
                        order.getId()
                );

        assertThat(result.getStatus())
                .isEqualTo(
                        OrderStatus.CANCELLED
                );

        verifyLockedRead(order.getId());
        verify(orderRepository).save(order);
    }

    @Test
    void shouldThrowWhenOrderForTransitionDoesNotExist() {
        UUID orderId = UUID.randomUUID();

        when(orderRepository
                .findByIdForUpdate(orderId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.confirm(orderId)
        ).isInstanceOf(
                OrderNotFoundException.class
        );

        verify(orderRepository, never())
                .save(any());
    }

    @Test
    void shouldNotSaveWhenTransitionIsInvalid() {
        Order order = createOrder(
                OrderStatus.CREATED
        );

        when(orderRepository
                .findByIdForUpdate(
                        order.getId()
                ))
                .thenReturn(Optional.of(order));

        assertThatThrownBy(() ->
                service.confirm(order.getId())
        ).isInstanceOf(
                InvalidOrderStateException.class
        );

        verify(orderRepository, never())
                .save(any());
    }

    private void mockLockedOrder(
            Order order
    ) {
        when(orderRepository
                .findByIdForUpdate(
                        order.getId()
                ))
                .thenReturn(Optional.of(order));

        when(orderRepository.save(order))
                .thenReturn(order);
    }

    private void verifyLockedRead(
            UUID orderId
    ) {
        verify(orderRepository)
                .findByIdForUpdate(orderId);

        verify(orderRepository, never())
                .findById(orderId);
    }

    private Order createOrder(
            OrderStatus status
    ) {
        return createOrder(
                UUID.randomUUID(),
                status
        );
    }

    private Order createOrder(
            UUID userId,
            OrderStatus status
    ) {
        return new Order(
                UUID.randomUUID(),
                userId,
                List.of(
                        new OrderItem(
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                1,
                                new BigDecimal("10.00")
                        )
                ),
                status,
                Instant.now()
        );
    }
}