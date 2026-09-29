package com.adrianperezcobo.dummycommerce.orders.order.adapter.in.web.response;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemResponse(
        UUID id,
        UUID productId,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal
) {
}