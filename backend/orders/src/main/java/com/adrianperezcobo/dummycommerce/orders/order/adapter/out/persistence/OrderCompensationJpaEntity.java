package com.adrianperezcobo.dummycommerce.orders.order.adapter.out.persistence;

import com.adrianperezcobo.dummycommerce.orders.order.domain.CompensationStatus;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "order_compensations")
public class OrderCompensationJpaEntity {
    @Id
    @Column(name = "order_id")
    private UUID orderId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CompensationStatus status;
    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;
    @Column(name = "last_error", length = 2000)
    private String lastError;
    @Column(name = "next_attempt_at")
    private Instant nextAttemptAt;

    protected OrderCompensationJpaEntity() { }
    public OrderCompensationJpaEntity(UUID orderId) {
        this.orderId = orderId;
        this.status = CompensationStatus.RELEASE_PENDING;
    }
    public UUID getOrderId() { return orderId; }
    public CompensationStatus getStatus() { return status; }
    public boolean retryDue(Instant now) {
        return status == CompensationStatus.REFUND_FAILED && nextAttemptAt != null && !nextAttemptAt.isAfter(now);
    }
    public void requestRefund() {
        status = CompensationStatus.REFUND_PENDING;
        attemptCount++;
        nextAttemptAt = null;
    }
    public void fail(String error, Instant nextAttemptAt) {
        status = CompensationStatus.REFUND_FAILED;
        lastError = error.substring(0, Math.min(2000, error.length()));
        this.nextAttemptAt = nextAttemptAt;
    }
    public void refunded() { status = CompensationStatus.RELEASE_PENDING; lastError = null; nextAttemptAt = null; }
    public void complete() { status = CompensationStatus.COMPLETED; }
}
