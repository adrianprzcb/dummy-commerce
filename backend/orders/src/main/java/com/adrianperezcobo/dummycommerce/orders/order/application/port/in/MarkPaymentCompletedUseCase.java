package com.adrianperezcobo.dummycommerce.orders.order.application.port.in;

import com.adrianperezcobo.dummycommerce.orders.order.domain.Order;

import java.util.UUID;

public interface MarkPaymentCompletedUseCase {

    Order markPaymentCompleted(UUID orderId);
}