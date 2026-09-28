package com.adrianperezcobo.dummycommerce.orders.order.application.port.out;

import com.adrianperezcobo.dummycommerce.orders.order.domain.Order;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository {

    Order save(Order order);

    Optional<Order> findById(UUID orderId);

    Optional<Order> findByIdForUpdate(
            UUID orderId
    );

    List<Order> findByUserId(
            UUID userId
    );
}