package com.adrianperezcobo.dummycommerce.orders.order.application.port.in;

import com.adrianperezcobo.dummycommerce.orders.order.domain.Order;

import java.util.UUID;

public interface GetOrderUseCase {

    Order getById(UUID orderId);
}