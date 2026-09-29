package com.adrianperezcobo.dummycommerce.orders.order.adapter.in.web.response;

import com.adrianperezcobo.dummycommerce.orders.order.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        UUID userId,
        OrderStatus status,
        List<OrderItemResponse> items,
        BigDecimal totalAmount,
        Instant createdAt
) {
}