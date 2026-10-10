package com.adrianperezcobo.dummycommerce.orders.order.application.port.out;

import com.adrianperezcobo.dummycommerce.orders.order.domain.CompensationStatus;
import java.time.Instant;
import java.util.UUID;

public interface OrderCompensationPort {
    boolean isRequested(UUID orderId);
    void request(UUID orderId);
    void requestRefund(UUID orderId);
    CompensationStatus status(UUID orderId);
    void refundFailed(UUID orderId, String error, Instant nextAttemptAt);
    void refunded(UUID orderId);
    void complete(UUID orderId);
    boolean retryDue(UUID orderId, Instant now);
    void retryRefund(UUID orderId);
}
