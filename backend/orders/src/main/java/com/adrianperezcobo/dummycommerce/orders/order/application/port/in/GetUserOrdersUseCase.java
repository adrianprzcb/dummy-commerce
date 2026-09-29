package com.adrianperezcobo.dummycommerce.orders.order.application.port.in;

import com.adrianperezcobo.dummycommerce.orders.order.domain.Order;

import java.util.List;
import java.util.UUID;

public interface GetUserOrdersUseCase {

    List<Order> getByUserId(UUID userId);
}