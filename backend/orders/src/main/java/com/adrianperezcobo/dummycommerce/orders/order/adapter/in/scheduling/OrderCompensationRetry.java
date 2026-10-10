package com.adrianperezcobo.dummycommerce.orders.order.adapter.in.scheduling;

import com.adrianperezcobo.dummycommerce.orders.order.adapter.out.persistence.OrderCompensationJpaRepository;
import com.adrianperezcobo.dummycommerce.orders.order.application.service.OrderSagaHandler;
import com.adrianperezcobo.dummycommerce.orders.order.domain.CompensationStatus;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.Instant;

@Component
@ConditionalOnProperty(name = "saga.refund.retry-enabled", havingValue = "true", matchIfMissing = true)
public class OrderCompensationRetry {
    private final OrderCompensationJpaRepository repository;
    private final OrderSagaHandler handler;
    public OrderCompensationRetry(OrderCompensationJpaRepository repository, OrderSagaHandler handler) {
        this.repository = repository;
        this.handler = handler;
    }

    @Scheduled(fixedDelayString = "${saga.refund.retry-delay-ms:60000}")
    public void retry() {
        for (var compensation : repository.findTop100ByStatusAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc(
                CompensationStatus.REFUND_FAILED, Instant.now())) {
            handler.retryRefund(compensation.getOrderId());
        }
    }
}
