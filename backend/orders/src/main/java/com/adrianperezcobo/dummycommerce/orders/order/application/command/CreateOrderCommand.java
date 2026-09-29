package com.adrianperezcobo.dummycommerce.orders.order.application.command;

import java.util.List;
import java.util.UUID;

public record CreateOrderCommand(
        UUID userId,
        List<CreateOrderItemCommand> items
) {
}