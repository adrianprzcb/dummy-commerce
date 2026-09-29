package com.adrianperezcobo.dummycommerce.orders.order.application.service;

import com.adrianperezcobo.dummycommerce.orders.order.application.command.CreateOrderCommand;
import com.adrianperezcobo.dummycommerce.orders.order.application.command.CreateOrderItemCommand;
import com.adrianperezcobo.dummycommerce.orders.order.application.exception.OrderNotFoundException;
import com.adrianperezcobo.dummycommerce.orders.order.application.port.in.*;
import com.adrianperezcobo.dummycommerce.orders.order.application.port.out.OrderRepository;
import com.adrianperezcobo.dummycommerce.orders.order.domain.Order;
import com.adrianperezcobo.dummycommerce.orders.order.domain.OrderItem;
import com.adrianperezcobo.dummycommerce.orders.order.domain.OrderStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class OrderService implements
        CreateOrderUseCase,
        GetOrderUseCase,
        GetUserOrdersUseCase,
        MarkStockReservedUseCase,
        MarkPaymentCompletedUseCase,
        ConfirmOrderUseCase,
        CancelOrderUseCase {

    private final OrderRepository orderRepository;

    public OrderService(
            OrderRepository orderRepository
    ) {
        this.orderRepository = orderRepository;
    }

    @Override
    @Transactional
    public Order create(
            CreateOrderCommand command
    ) {
        List<OrderItem> items =
                command.items()
                        .stream()
                        .map(this::createOrderItem)
                        .toList();

        Order order = new Order(
                UUID.randomUUID(),
                command.userId(),
                items,
                OrderStatus.CREATED,
                Instant.now()
        );

        return orderRepository.save(order);
    }

    @Override
    @Transactional(readOnly = true)
    public Order getById(
            UUID orderId
    ) {
        return orderRepository
                .findById(orderId)
                .orElseThrow(
                        () -> new OrderNotFoundException(
                                orderId
                        )
                );
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> getByUserId(
            UUID userId
    ) {
        return orderRepository.findByUserId(
                userId
        );
    }

    @Override
    @Transactional
    public Order markStockReserved(
            UUID orderId
    ) {
        Order order = getForUpdate(orderId);

        order.markStockReserved();

        return orderRepository.save(order);
    }

    @Override
    @Transactional
    public Order markPaymentCompleted(
            UUID orderId
    ) {
        Order order = getForUpdate(orderId);

        order.markPaymentCompleted();

        return orderRepository.save(order);
    }

    @Override
    @Transactional
    public Order confirm(
            UUID orderId
    ) {
        Order order = getForUpdate(orderId);

        order.confirm();

        return orderRepository.save(order);
    }

    @Override
    @Transactional
    public Order cancel(
            UUID orderId
    ) {
        Order order = getForUpdate(orderId);

        order.cancel();

        return orderRepository.save(order);
    }

    private Order getForUpdate(
            UUID orderId
    ) {
        return orderRepository
                .findByIdForUpdate(orderId)
                .orElseThrow(
                        () -> new OrderNotFoundException(
                                orderId
                        )
                );
    }

    private OrderItem createOrderItem(
            CreateOrderItemCommand command
    ) {
        return new OrderItem(
                UUID.randomUUID(),
                command.productId(),
                command.quantity(),
                command.unitPrice()
        );
    }
}