package com.adrianperezcobo.dummycommerce.orders.order.adapter.out.persistence;

import com.adrianperezcobo.dummycommerce.orders.order.application.port.out.OrderCompensationPort;
import com.adrianperezcobo.dummycommerce.orders.order.domain.CompensationStatus;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.UUID;

@Component
public class OrderCompensationAdapter implements OrderCompensationPort {
    private final OrderCompensationJpaRepository repository;
    public OrderCompensationAdapter(OrderCompensationJpaRepository repository) { this.repository = repository; }
    public boolean isRequested(UUID orderId) { return repository.existsById(orderId); }
    public void request(UUID orderId) { repository.save(new OrderCompensationJpaEntity(orderId)); }
    public void requestRefund(UUID orderId) {
        var compensation = new OrderCompensationJpaEntity(orderId);
        compensation.requestRefund();
        repository.save(compensation);
    }
    public CompensationStatus status(UUID orderId) { return get(orderId).getStatus(); }
    public void refundFailed(UUID orderId, String error, Instant nextAttemptAt) {
        var compensation = get(orderId);
        compensation.fail(error, nextAttemptAt);
        repository.save(compensation);
    }
    public void refunded(UUID orderId) { var c = get(orderId); c.refunded(); repository.save(c); }
    public void complete(UUID orderId) { var c = get(orderId); c.complete(); repository.save(c); }
    public boolean retryDue(UUID orderId, Instant now) { return get(orderId).retryDue(now); }
    public void retryRefund(UUID orderId) { var c = get(orderId); c.requestRefund(); repository.save(c); }
    private OrderCompensationJpaEntity get(UUID orderId) { return repository.findById(orderId).orElseThrow(); }
}
