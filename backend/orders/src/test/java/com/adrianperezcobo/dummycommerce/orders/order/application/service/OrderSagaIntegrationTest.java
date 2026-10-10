package com.adrianperezcobo.dummycommerce.orders.order.application.service;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.AopTestUtils;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.json.JsonMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.adrianperezcobo.dummycommerce.orders.order.application.command.*;
import com.adrianperezcobo.dummycommerce.orders.order.application.integration.inventory.*;
import com.adrianperezcobo.dummycommerce.orders.order.application.integration.payments.*;
import com.adrianperezcobo.dummycommerce.orders.order.application.integration.orders.*;
import com.adrianperezcobo.dummycommerce.orders.order.domain.Order;
import com.adrianperezcobo.dummycommerce.orders.order.domain.OrderStatus;
import com.adrianperezcobo.dummycommerce.orders.shared.outbox.OutboxPort;
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
class OrderSagaIntegrationTest {
    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");
    @Autowired JdbcTemplate jdbc;
    @Autowired JsonMapper json;
    @MockitoSpyBean OutboxPort outbox;

    @Autowired OrderService service;
    @Autowired OrderSagaHandler handler;

    @Test
    void happyPathWritesCommandsAndConfirmsOnlyAfterStockConfirmed() {
        Order order = create();
        UUID id = order.getId();
        ReserveStockCommandV1 reserve = event(id, InventoryTopics.RESERVE_STOCK_V1, ReserveStockCommandV1.class);
        assertThat(reserve.items()).containsExactly(new ReserveStockCommandV1.Item(order.getItems().getFirst().getProductId(), 2));

        StockReservedEventV1 stock = new StockReservedEventV1(UUID.randomUUID(), id, Instant.now());
        handler.handle(stock);
        assertThat(service.getById(id).getStatus()).isEqualTo(OrderStatus.STOCK_RESERVED);
        ProcessPaymentCommandV1 payment = event(id, PaymentsTopics.PROCESS_PAYMENT_V1, ProcessPaymentCommandV1.class);
        assertThat(payment.amount()).isEqualByComparingTo("24.68");
        assertThat(payment.currency()).isEqualTo(PlatformCurrency.EUR);
        handler.handle(stock);
        assertThat(count("outbox_messages", "aggregate_id", id)).isEqualTo(2);

        PaymentCompletedEventV1 paid = new PaymentCompletedEventV1(UUID.randomUUID(), id, UUID.randomUUID(), Instant.now(), payment.amount(), payment.currency());
        handler.handle(paid);
        handler.handle(paid);
        assertThat(service.getById(id).getStatus()).isEqualTo(OrderStatus.PAYMENT_COMPLETED);
        event(id, InventoryTopics.CONFIRM_STOCK_V1, ConfirmStockCommandV1.class);

        StockConfirmedEventV1 confirmed = new StockConfirmedEventV1(UUID.randomUUID(), id, Instant.now());
        handler.handle(confirmed);
        handler.handle(confirmed);
        assertThat(service.getById(id).getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        OrderConfirmedEventV1 finalEvent = event(id, OrdersTopics.ORDER_CONFIRMED_V1, OrderConfirmedEventV1.class);
        assertThat(finalEvent.userId()).isEqualTo(order.getUserId());
        assertThat(count("outbox_messages", "aggregate_id", id)).isEqualTo(4);
    }

    @Test
    void paymentFailureWaitsForReleaseBeforeCancelling() {
        Order order = create();
        UUID id = order.getId();
        handler.handle(new StockReservedEventV1(UUID.randomUUID(), id, Instant.now()));
        PaymentFailedEventV1 failure = new PaymentFailedEventV1(UUID.randomUUID(), id, UUID.randomUUID(), Instant.now(), order.getTotalAmount(), PlatformCurrency.EUR, "declined");
        handler.handle(failure);
        handler.handle(failure);
        assertThat(service.getById(id).getStatus()).isEqualTo(OrderStatus.STOCK_RESERVED);
        assertThat(count("order_compensations", "order_id", id)).isEqualTo(1);
        event(id, InventoryTopics.RELEASE_STOCK_V1, ReleaseStockCommandV1.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM outbox_messages WHERE aggregate_id = ? AND topic = ?", Long.class, id, OrdersTopics.ORDER_CANCELLED_V1)).isZero();
        StockReleasedEventV1 released = new StockReleasedEventV1(UUID.randomUUID(), id, Instant.now());
        handler.handle(released);
        handler.handle(released);
        assertThat(service.getById(id).getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(event(id, OrdersTopics.ORDER_CANCELLED_V1, OrderCancelledEventV1.class).reason()).isEqualTo("PAYMENT_FAILED");
        assertThat(count("outbox_messages", "aggregate_id", id)).isEqualTo(4);
    }

    @Test
    void stockFailureCancelsWithUsefulReason() {
        Order order = create();
        StockReservationFailedEventV1 failure = new StockReservationFailedEventV1(UUID.randomUUID(), order.getId(), Instant.now(), "INSUFFICIENT_STOCK");
        handler.handle(failure);
        handler.handle(failure);
        assertThat(service.getById(order.getId()).getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(event(order.getId(), OrdersTopics.ORDER_CANCELLED_V1, OrderCancelledEventV1.class).reason()).contains("STOCK_RESERVATION_FAILED", "INSUFFICIENT_STOCK");
        assertThat(count("outbox_messages", "aggregate_id", order.getId())).isEqualTo(2);
    }

    @Test
    void outboxFailureRollsBackOrderAndInboxThenRedeliverySucceeds() {
        Order order = create();
        StockReservedEventV1 stock = new StockReservedEventV1(UUID.randomUUID(), order.getId(), Instant.now());
        doThrow(new IllegalStateException("outbox unavailable")).when(outboxSpy()).save(any(), any(), anyString(), anyString(), any(), any());
        assertThatThrownBy(() -> handler.handle(stock)).isInstanceOf(IllegalStateException.class);
        assertThat(service.getById(order.getId()).getStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(count("inbox_messages", "message_id", stock.messageId())).isZero();
        assertThat(count("outbox_messages", "aggregate_id", order.getId())).isEqualTo(1);
        reset(outboxSpy());
        handler.handle(stock);
        assertThat(service.getById(order.getId()).getStatus()).isEqualTo(OrderStatus.STOCK_RESERVED);
    }

    @Test
    void createFailureRollsBackOrderAndItems() {
        long before = jdbc.queryForObject("SELECT COUNT(*) FROM orders", Long.class);
        doThrow(new IllegalStateException("outbox unavailable")).when(outboxSpy()).save(any(), any(), anyString(), anyString(), any(), any());
        assertThatThrownBy(this::create).isInstanceOf(IllegalStateException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM orders", Long.class)).isEqualTo(before);
    }

    @Test
    void releaseWithoutCompensationRollsBackInboxAndDoesNotCancel() {
        Order order = create();
        handler.handle(new StockReservedEventV1(UUID.randomUUID(), order.getId(), Instant.now()));
        StockReleasedEventV1 release = new StockReleasedEventV1(UUID.randomUUID(), order.getId(), Instant.now());
        assertThatThrownBy(() -> handler.handle(release)).isInstanceOf(RuntimeException.class);
        assertThat(service.getById(order.getId()).getStatus()).isEqualTo(OrderStatus.STOCK_RESERVED);
        assertThat(count("inbox_messages", "message_id", release.messageId())).isZero();
    }

    @Test
    @Timeout(30)
    void concurrentDuplicateDeliveryAppliesTransitionOnce() throws Exception {
        Order order = create();
        StockReservedEventV1 stock = new StockReservedEventV1(UUID.randomUUID(), order.getId(), Instant.now());
        try (var executor = java.util.concurrent.Executors.newFixedThreadPool(4)) {
            var start = new java.util.concurrent.CountDownLatch(1);
            List<java.util.concurrent.Future<?>> tasks = new ArrayList<>();
            for (int i = 0; i < 4; i++) tasks.add(executor.submit(() -> { start.await(); handler.handle(stock); return null; }));
            start.countDown();
            for (var task : tasks) task.get(20, java.util.concurrent.TimeUnit.SECONDS);
        }
        assertThat(count("inbox_messages", "message_id", stock.messageId())).isEqualTo(1);
        assertThat(count("outbox_messages", "aggregate_id", order.getId())).isEqualTo(2);
    }

    @Test
    void permanentStockConfirmationFailureRefundsBeforeReleasingAndCancelling() {
        Order order = paidOrder();
        UUID id = order.getId();
        var failure = new StockConfirmationFailedEventV1(UUID.randomUUID(), id, Instant.now(), "RESERVATIONS_RELEASED");
        handler.handle(failure);
        handler.handle(failure);
        event(id, PaymentsTopics.REFUND_PAYMENT_V1, RefundPaymentCommandV1.class);
        assertThat(service.getById(id).getStatus()).isEqualTo(OrderStatus.PAYMENT_COMPLETED);
        assertThat(jdbc.queryForObject("SELECT status FROM order_compensations WHERE order_id = ?", String.class, id)).isEqualTo("REFUND_PENDING");
        var premature = new StockReleasedEventV1(UUID.randomUUID(), id, Instant.now());
        assertThatThrownBy(() -> handler.handle(premature)).isInstanceOf(RuntimeException.class);
        assertThat(count("inbox_messages", "message_id", premature.messageId())).isZero();
        var refunded = new PaymentRefundedEventV1(UUID.randomUUID(), id, UUID.randomUUID(), Instant.now(), order.getTotalAmount(), PlatformCurrency.EUR);
        handler.handle(refunded);
        handler.handle(refunded);
        handler.handle(new PaymentRefundedEventV1(UUID.randomUUID(), id, refunded.paymentId(), Instant.now(), order.getTotalAmount(), PlatformCurrency.EUR));
        event(id, InventoryTopics.RELEASE_STOCK_V1, ReleaseStockCommandV1.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM outbox_messages WHERE aggregate_id = ? AND topic = ?", Long.class, id, InventoryTopics.RELEASE_STOCK_V1)).isEqualTo(1);
        assertThat(service.getById(id).getStatus()).isEqualTo(OrderStatus.PAYMENT_COMPLETED);
        var released = new StockReleasedEventV1(UUID.randomUUID(), id, Instant.now());
        handler.handle(released);
        handler.handle(released);
        handler.handle(new StockReleasedEventV1(UUID.randomUUID(), id, Instant.now()));
        assertThat(service.getById(id).getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(event(id, OrdersTopics.ORDER_CANCELLED_V1, OrderCancelledEventV1.class).reason()).isEqualTo("STOCK_CONFIRMATION_FAILED");
        assertThat(jdbc.queryForObject("SELECT status FROM order_compensations WHERE order_id = ?", String.class, id)).isEqualTo("COMPLETED");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM outbox_messages WHERE aggregate_id = ? AND topic = ?", Long.class, id, OrdersTopics.ORDER_CANCELLED_V1)).isEqualTo(1);
    }

    @Test
    void failedRefundRemainsDurableAndRetriesOnlyWhenDue() {
        Order order = paidOrder();
        UUID id = order.getId();
        handler.handle(new StockConfirmationFailedEventV1(UUID.randomUUID(), id, Instant.now(), "RESERVATIONS_NOT_FOUND"));
        var failure = new PaymentRefundFailedEventV1(UUID.randomUUID(), id, UUID.randomUUID(), Instant.now(), order.getTotalAmount(), PlatformCurrency.EUR, "provider rejected");
        handler.handle(failure);
        handler.handle(failure);
        assertThat(service.getById(id).getStatus()).isEqualTo(OrderStatus.PAYMENT_COMPLETED);
        assertThat(jdbc.queryForObject("SELECT status FROM order_compensations WHERE order_id = ?", String.class, id)).isEqualTo("REFUND_FAILED");
        assertThat(jdbc.queryForObject("SELECT last_error FROM order_compensations WHERE order_id = ?", String.class, id)).isEqualTo("provider rejected");
        handler.retryRefund(id);
        assertThat(jdbc.queryForObject("SELECT attempt_count FROM order_compensations WHERE order_id = ?", Integer.class, id)).isEqualTo(1);
        jdbc.update("UPDATE order_compensations SET next_attempt_at = ? WHERE order_id = ?", java.sql.Timestamp.from(Instant.now().minusSeconds(1)), id);
        handler.retryRefund(id);
        handler.retryRefund(id);
        assertThat(jdbc.queryForObject("SELECT attempt_count FROM order_compensations WHERE order_id = ?", Integer.class, id)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM outbox_messages WHERE aggregate_id = ? AND topic = ?", Long.class, id, PaymentsTopics.REFUND_PAYMENT_V1)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM outbox_messages WHERE aggregate_id = ? AND topic = ?", Long.class, id, OrdersTopics.ORDER_CANCELLED_V1)).isZero();
    }

    @Test
    void waitingForStockConfirmationDoesNotStartRefundWithoutBusinessFailure() {
        Order order = paidOrder();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM outbox_messages WHERE aggregate_id = ? AND topic = ?", Long.class, order.getId(), PaymentsTopics.REFUND_PAYMENT_V1)).isZero();
        assertThat(count("order_compensations", "order_id", order.getId())).isZero();
    }

    @Test
    void refundOutboxFailureRollsBackCompensationAndInbox() {
        Order order = paidOrder();
        var failure = new StockConfirmationFailedEventV1(UUID.randomUUID(), order.getId(), Instant.now(), "RESERVATIONS_RELEASED");
        doThrow(new IllegalStateException("outbox unavailable")).when(outboxSpy()).save(any(), any(), anyString(), anyString(), any(), any());
        assertThatThrownBy(() -> handler.handle(failure)).isInstanceOf(IllegalStateException.class);
        assertThat(count("order_compensations", "order_id", order.getId())).isZero();
        assertThat(count("inbox_messages", "message_id", failure.messageId())).isZero();
    }

    private Order paidOrder() {
        Order order = create();
        handler.handle(new StockReservedEventV1(UUID.randomUUID(), order.getId(), Instant.now()));
        handler.handle(new PaymentCompletedEventV1(UUID.randomUUID(), order.getId(), UUID.randomUUID(), Instant.now(), order.getTotalAmount(), PlatformCurrency.EUR));
        return service.getById(order.getId());
    }

    private Order create() {
        return service.create(new CreateOrderCommand(UUID.randomUUID(), List.of(new CreateOrderItemCommand(UUID.randomUUID(), 2, new BigDecimal("12.34")))));
    }

    @Test
    void zeroTotalDoesNotPersistOrderItemsOrOutbox() {
        UUID userId = UUID.randomUUID();
        long ordersBefore = jdbc.queryForObject("SELECT COUNT(*) FROM orders", Long.class);
        long itemsBefore = jdbc.queryForObject("SELECT COUNT(*) FROM order_items", Long.class);
        long outboxBefore = jdbc.queryForObject("SELECT COUNT(*) FROM outbox_messages", Long.class);

        assertThatThrownBy(() -> service.create(new CreateOrderCommand(
                userId,
                List.of(new CreateOrderItemCommand(UUID.randomUUID(), 1, BigDecimal.ZERO))
        ))).isInstanceOf(IllegalArgumentException.class);

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM orders", Long.class)).isEqualTo(ordersBefore);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM order_items", Long.class)).isEqualTo(itemsBefore);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM outbox_messages", Long.class)).isEqualTo(outboxBefore);
    }

    private OutboxPort outboxSpy() {
        return AopTestUtils.getUltimateTargetObject(outbox);
    }

    private long count(String table, String column, UUID id) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE " + column + " = ?", Long.class, id);
    }

    private <T> T event(UUID orderId, String topic, Class<T> type) {
        Map<String, Object> row = jdbc.queryForMap("SELECT * FROM outbox_messages WHERE aggregate_id = ? AND topic = ? ORDER BY created_at DESC LIMIT 1", orderId, topic);
        assertThat(row.get("message_key")).isEqualTo(orderId.toString());
        assertThat(row.get("status")).isEqualTo("PENDING");
        String payload = (String) row.get("payload");
        var tree = json.readTree(payload);
        assertThat(tree.get("messageId").asText()).isEqualTo(row.get("id").toString());
        assertThat(tree.get("orderId").asText()).isEqualTo(orderId.toString());
        assertThat(tree.get("occurredAt").asText()).isNotBlank();
        return json.readValue(payload, type);
    }
}
