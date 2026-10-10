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
import java.util.UUID;

@Component
public class OrderWebMapper {
    private final com.adrianperezcobo.dummycommerce.orders.order.application.port.out.ProductCatalogPort catalog;

    public OrderWebMapper(com.adrianperezcobo.dummycommerce.orders.order.application.port.out.ProductCatalogPort catalog) {
        this.catalog = catalog;
    }


    public CreateOrderCommand toCommand(
            CreateOrderRequest request, UUID userId
    ) {
        return new CreateOrderCommand(
                userId,
                request.items()
                        .stream()
                        .map(this::toCommand)
                        .toList()
        );
    }

    private CreateOrderItemCommand toCommand(
            CreateOrderItemRequest request
    ) {
        var price = catalog.findActivePrice(request.productId()).orElseThrow(() ->
                new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT, "Product is unavailable"));
        if (price.compareTo(request.unitPrice()) != 0) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT, "Product price changed; refresh catalogue");
        }
        return new CreateOrderItemCommand(
                request.productId(),
                request.quantity(),
                price
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