package com.adrianperezcobo.dummycommerce.orders.order.application.port.in;

import com.adrianperezcobo.dummycommerce.orders.order.application.command.CreateOrderCommand;
import com.adrianperezcobo.dummycommerce.orders.order.domain.Order;

public interface CreateOrderUseCase {

    Order create(CreateOrderCommand command);
}