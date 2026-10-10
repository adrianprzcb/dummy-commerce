package com.adrianperezcobo.dummycommerce.orders.order.application.service;

import com.adrianperezcobo.dummycommerce.orders.order.application.integration.inventory.*;
import com.adrianperezcobo.dummycommerce.orders.order.application.integration.payments.*;
import com.adrianperezcobo.dummycommerce.orders.order.application.integration.orders.*;
import com.adrianperezcobo.dummycommerce.orders.order.application.exception.OrderNotFoundException;
import com.adrianperezcobo.dummycommerce.orders.order.application.port.out.*;
import com.adrianperezcobo.dummycommerce.orders.order.domain.*;
import com.adrianperezcobo.dummycommerce.orders.order.domain.exception.InvalidOrderStateException;
import com.adrianperezcobo.dummycommerce.orders.shared.inbox.InboxPort;
import com.adrianperezcobo.dummycommerce.orders.shared.outbox.OutboxPort;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.UUID;

@Service
@Transactional
public class OrderSagaHandler {
    private final OrderRepository orders;
    private final OrderCompensationPort compensations;
    private final InboxPort inbox;
    private final OutboxPort outbox;
    private final long refundRetryDelayMs;

    public OrderSagaHandler(OrderRepository orders, OrderCompensationPort compensations, InboxPort inbox, OutboxPort outbox,
            @Value("${saga.refund.retry-delay-ms:60000}") long refundRetryDelayMs) {
        this.orders = orders;
        this.compensations = compensations;
        this.inbox = inbox;
        this.outbox = outbox;
        this.refundRetryDelayMs = refundRetryDelayMs;
        if (refundRetryDelayMs < 1) throw new IllegalArgumentException("Invalid refund retry delay");
    }

    public void handle(StockReservedEventV1 event) {
        if (!inbox.claim(event.messageId(), InventoryTopics.STOCK_RESERVED_V1)) return;
        Order order = locked(event.orderId());
        if (order.getStatus() != OrderStatus.CREATED) return;
        order.markStockReserved();
        orders.save(order);
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        publish(id, order, PaymentsTopics.PROCESS_PAYMENT_V1,
                new ProcessPaymentCommandV1(id, order.getId(), now, order.getTotalAmount(), PlatformCurrency.EUR), now);
    }

    public void handle(StockReservationFailedEventV1 event) {
        if (!inbox.claim(event.messageId(), InventoryTopics.STOCK_RESERVATION_FAILED_V1)) return;
        Order order = locked(event.orderId());
        if (order.getStatus() == OrderStatus.CANCELLED) return;
        requireStatus(order, OrderStatus.CREATED);
        cancel(order, "STOCK_RESERVATION_FAILED: " + event.reason());
    }

    public void handle(PaymentCompletedEventV1 event) {
        if (!inbox.claim(event.messageId(), PaymentsTopics.PAYMENT_COMPLETED_V1)) return;
        Order order = locked(event.orderId());
        if (order.getStatus() == OrderStatus.PAYMENT_COMPLETED || order.getStatus() == OrderStatus.CONFIRMED) return;
        requireStatus(order, OrderStatus.STOCK_RESERVED);
        if (compensations.isRequested(order.getId())) throw new InvalidOrderStateException("Payment completion after compensation requested");
        validatePayment(order, event.amount(), event.currency());
        order.markPaymentCompleted();
        orders.save(order);
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        publish(id, order, InventoryTopics.CONFIRM_STOCK_V1, new ConfirmStockCommandV1(id, order.getId(), now), now);
    }

    public void handle(PaymentFailedEventV1 event) {
        if (!inbox.claim(event.messageId(), PaymentsTopics.PAYMENT_FAILED_V1)) return;
        Order order = locked(event.orderId());
        if (compensations.isRequested(order.getId())) return;
        requireStatus(order, OrderStatus.STOCK_RESERVED);
        validatePayment(order, event.amount(), event.currency());
        compensations.request(order.getId());
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        publish(id, order, InventoryTopics.RELEASE_STOCK_V1, new ReleaseStockCommandV1(id, order.getId(), now), now);
    }

    public void handle(StockConfirmedEventV1 event) {
        if (!inbox.claim(event.messageId(), InventoryTopics.STOCK_CONFIRMED_V1)) return;
        Order order = locked(event.orderId());
        if (order.getStatus() == OrderStatus.CONFIRMED) return;
        if (compensations.isRequested(order.getId())) throw new InvalidOrderStateException("Stock confirmation during compensation");
        order.confirm();
        orders.save(order);
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        publish(id, order, OrdersTopics.ORDER_CONFIRMED_V1, new OrderConfirmedEventV1(id, order.getId(), order.getUserId(), now), now);
    }

    public void handle(StockReleasedEventV1 event) {
        if (!inbox.claim(event.messageId(), InventoryTopics.STOCK_RELEASED_V1)) return;
        Order order = locked(event.orderId());
        if (order.getStatus() == OrderStatus.CANCELLED) return;
        if (!compensations.isRequested(order.getId()) || compensations.status(order.getId()) != CompensationStatus.RELEASE_PENDING) {
            throw new InvalidOrderStateException("Stock release before compensation is ready");
        }
        if (order.getStatus() != OrderStatus.STOCK_RESERVED && order.getStatus() != OrderStatus.PAYMENT_COMPLETED) {
            throw new InvalidOrderStateException("Unexpected compensated order status: " + order.getStatus());
        }
        String reason = order.getStatus() == OrderStatus.PAYMENT_COMPLETED ? "STOCK_CONFIRMATION_FAILED" : "PAYMENT_FAILED";
        compensations.complete(order.getId());
        cancel(order, reason);
    }

    public void handle(StockConfirmationFailedEventV1 event) {
        if (!inbox.claim(event.messageId(), InventoryTopics.STOCK_CONFIRMATION_FAILED_V1)) return;
        Order order = locked(event.orderId());
        if (compensations.isRequested(order.getId())) return;
        requireStatus(order, OrderStatus.PAYMENT_COMPLETED);
        compensations.requestRefund(order.getId());
        requestRefund(order);
    }

    public void handle(PaymentRefundedEventV1 event) {
        if (!inbox.claim(event.messageId(), PaymentsTopics.PAYMENT_REFUNDED_V1)) return;
        Order order = locked(event.orderId());
        if (order.getStatus() == OrderStatus.CANCELLED) return;
        requireStatus(order, OrderStatus.PAYMENT_COMPLETED);
        validatePayment(order, event.amount(), event.currency());
        if (!compensations.isRequested(order.getId())) throw new InvalidOrderStateException("Unrequested refund");
        if (compensations.status(order.getId()) == CompensationStatus.RELEASE_PENDING) return;
        compensations.refunded(order.getId());
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        publish(id, order, InventoryTopics.RELEASE_STOCK_V1, new ReleaseStockCommandV1(id, order.getId(), now), now);
    }

    public void handle(PaymentRefundFailedEventV1 event) {
        if (!inbox.claim(event.messageId(), PaymentsTopics.PAYMENT_REFUND_FAILED_V1)) return;
        Order order = locked(event.orderId());
        if (order.getStatus() == OrderStatus.CANCELLED) return;
        requireStatus(order, OrderStatus.PAYMENT_COMPLETED);
        validatePayment(order, event.amount(), event.currency());
        if (!compensations.isRequested(order.getId())) throw new InvalidOrderStateException("Unrequested refund failure");
        if (compensations.status(order.getId()) != CompensationStatus.REFUND_PENDING) return;
        compensations.refundFailed(order.getId(), event.reason(), Instant.now().plusMillis(refundRetryDelayMs));
    }

    public void retryRefund(UUID orderId) {
        Order order = locked(orderId);
        if (order.getStatus() != OrderStatus.PAYMENT_COMPLETED || !compensations.retryDue(orderId, Instant.now())) return;
        compensations.retryRefund(orderId);
        requestRefund(order);
    }

    private void requestRefund(Order order) {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        publish(id, order, PaymentsTopics.REFUND_PAYMENT_V1, new RefundPaymentCommandV1(id, order.getId(), now), now);
    }

    private void cancel(Order order, String reason) {
        order.cancel();
        orders.save(order);
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        publish(id, order, OrdersTopics.ORDER_CANCELLED_V1, new OrderCancelledEventV1(id, order.getId(), order.getUserId(), now, reason), now);
    }

    private Order locked(UUID id) {
        return orders.findByIdForUpdate(id).orElseThrow(() -> new OrderNotFoundException(id));
    }

    private void requireStatus(Order order, OrderStatus expected) {
        if (order.getStatus() != expected) throw new InvalidOrderStateException("Order must be " + expected + " but is " + order.getStatus());
    }

    private void validatePayment(Order order, java.math.BigDecimal amount, String currency) {
        if (order.getTotalAmount().compareTo(amount) != 0 || !PlatformCurrency.EUR.equals(currency)) {
            throw new IllegalArgumentException("Payment does not match order amount/currency");
        }
    }

    private void publish(UUID id, Order order, String topic, Object payload, Instant now) {
        outbox.save(id, order.getId(), topic, order.getId().toString(), payload, now);
    }
}
