package com.adrianperezcobo.dummycommerce.orders.order.adapter.in.web;

import com.adrianperezcobo.dummycommerce.orders.order.adapter.in.web.request.CreateOrderItemRequest;
import com.adrianperezcobo.dummycommerce.orders.order.adapter.in.web.request.CreateOrderRequest;
import com.adrianperezcobo.dummycommerce.orders.order.adapter.in.web.response.OrderItemResponse;
import com.adrianperezcobo.dummycommerce.orders.order.adapter.in.web.response.OrderResponse;
import com.adrianperezcobo.dummycommerce.orders.order.application.command.CreateOrderCommand;
import com.adrianperezcobo.dummycommerce.orders.order.application.command.CreateOrderItemCommand;
import com.adrianperezcobo.dummycommerce.orders.order.domain.Order;
import com.adrianperezcobo.dummycommerce.orders.order.domain.OrderItem;
import org.springframework.stereotype.Component;

@Component
public class OrderWebMapper {

    public CreateOrderCommand toCommand(
            CreateOrderRequest request
    ) {
        return new CreateOrderCommand(
                request.userId(),
                request.items()
                        .stream()
                        .map(this::toCommand)
                        .toList()
        );
    }

    private CreateOrderItemCommand toCommand(
            CreateOrderItemRequest request
    ) {
        return new CreateOrderItemCommand(
                request.productId(),
                request.quantity(),
                request.unitPrice()
        );
    }

    public OrderResponse toResponse(
            Order order
    ) {
        return new OrderResponse(
                order.getId(),
                order.getUserId(),
                order.getStatus(),
                order.getItems()
                        .stream()
                        .map(this::toResponse)
                        .toList(),
                order.getTotalAmount(),
                order.getCreatedAt()
        );
    }

    private OrderItemResponse toResponse(
            OrderItem item
    ) {
        return new OrderItemResponse(
                item.getId(),
                item.getProductId(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getSubtotal()
        );
    }
}