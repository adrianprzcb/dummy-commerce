package com.adrianperezcobo.dummycommerce.inventory.reservation.application.service;

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

import com.adrianperezcobo.dummycommerce.inventory.reservation.application.integration.inventory.*;
import com.adrianperezcobo.dummycommerce.inventory.stock.application.port.out.InventoryRepository;
import com.adrianperezcobo.dummycommerce.inventory.stock.domain.InventoryItem;
import com.adrianperezcobo.dummycommerce.inventory.shared.outbox.OutboxPort;
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
class InventorySagaIntegrationTest {
    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");
    @Autowired JdbcTemplate jdbc;
    @Autowired JsonMapper json;
    @MockitoSpyBean OutboxPort outbox;

    @Autowired InventorySagaHandler handler;
    @Autowired InventoryRepository inventory;

    @Test
    void reservesAllItemsAndConfirmsAllWithoutDoubleApplying() {
        UUID first = stock(5), second = stock(6);
        ReserveStockCommandV1 command = command(first, second);
        handler.handle(command);
        handler.handle(command);
        assertStock(first, 3, 2);
        assertStock(second, 3, 3);
        assertThat(count("stock_reservations", "order_id", command.orderId())).isEqualTo(2);
        event(command.orderId(), InventoryTopics.STOCK_RESERVED_V1, StockReservedEventV1.class);
        assertThat(count("outbox_messages", "aggregate_id", command.orderId())).isEqualTo(1);
        ConfirmStockCommandV1 confirm = new ConfirmStockCommandV1(UUID.randomUUID(), command.orderId(), Instant.now());
        handler.handle(confirm);
        handler.handle(confirm);
        assertStock(first, 3, 0);
        assertStock(second, 3, 0);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM stock_reservations WHERE order_id = ? AND status = 'CONFIRMED'", Long.class, command.orderId())).isEqualTo(2);
        event(command.orderId(), InventoryTopics.STOCK_CONFIRMED_V1, StockConfirmedEventV1.class);
    }

    @Test
    void insufficientStockLeavesEntireBatchUnchangedAndEmitsFailure() {
        UUID first = stock(5), second = stock(1);
        ReserveStockCommandV1 command = command(first, second);
        handler.handle(command);
        handler.handle(command);
        assertStock(first, 5, 0);
        assertStock(second, 1, 0);
        assertThat(count("stock_reservations", "order_id", command.orderId())).isZero();
        assertThat(event(command.orderId(), InventoryTopics.STOCK_RESERVATION_FAILED_V1, StockReservationFailedEventV1.class).reason()).contains("INSUFFICIENT_STOCK", second.toString());
        assertThat(count("outbox_messages", "aggregate_id", command.orderId())).isEqualTo(1);
    }

    @Test
    void missingProductAlsoLeavesEntireBatchUnchanged() {
        UUID first = stock(5);
        ReserveStockCommandV1 command = command(first, UUID.randomUUID());
        handler.handle(command);
        assertStock(first, 5, 0);
        assertThat(count("stock_reservations", "order_id", command.orderId())).isZero();
        assertThat(event(command.orderId(), InventoryTopics.STOCK_RESERVATION_FAILED_V1, StockReservationFailedEventV1.class).reason()).contains("INVENTORY_NOT_FOUND");
    }

    @Test
    void releasesAllAndRepeatedBusinessCommandDoesNotReleaseTwice() {
        UUID first = stock(5), second = stock(6);
        ReserveStockCommandV1 command = command(first, second);
        handler.handle(command);
        ReleaseStockCommandV1 release = new ReleaseStockCommandV1(UUID.randomUUID(), command.orderId(), Instant.now());
        handler.handle(release);
        handler.handle(release);
        handler.handle(new ReleaseStockCommandV1(UUID.randomUUID(), command.orderId(), Instant.now()));
        assertStock(first, 5, 0);
        assertStock(second, 6, 0);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM stock_reservations WHERE order_id = ? AND status = 'RELEASED'", Long.class, command.orderId())).isEqualTo(2);
        event(command.orderId(), InventoryTopics.STOCK_RELEASED_V1, StockReleasedEventV1.class);
    }

    @Test
    void outboxFailureRollsBackEntireBatchAndInbox() {
        UUID first = stock(5), second = stock(6);
        ReserveStockCommandV1 command = command(first, second);
        doThrow(new IllegalStateException("outbox unavailable")).when(outboxSpy()).save(any(), any(), anyString(), anyString(), any(), any());
        assertThatThrownBy(() -> handler.handle(command)).isInstanceOf(IllegalStateException.class);
        assertStock(first, 5, 0);
        assertStock(second, 6, 0);
        assertThat(count("stock_reservations", "order_id", command.orderId())).isZero();
        assertThat(count("inbox_messages", "message_id", command.messageId())).isZero();
        reset(outboxSpy());
        handler.handle(command);
        assertStock(first, 3, 2);
    }

    @Test
    void cannotAcknowledgeReleaseOfConfirmedStock() {
        ReserveStockCommandV1 command = command(stock(5), stock(6));
        handler.handle(command);
        handler.handle(new ConfirmStockCommandV1(UUID.randomUUID(), command.orderId(), Instant.now()));
        ReleaseStockCommandV1 release = new ReleaseStockCommandV1(UUID.randomUUID(), command.orderId(), Instant.now());
        assertThatThrownBy(() -> handler.handle(release)).isInstanceOf(IllegalStateException.class);
        assertThat(count("inbox_messages", "message_id", release.messageId())).isZero();
    }

    @Test
    @Timeout(30)
    void overlappingBatchesWithReversedItemsDoNotOversell() throws Exception {
        UUID first = stock(5), second = stock(5);
        try (var executor = java.util.concurrent.Executors.newFixedThreadPool(8)) {
            var start = new java.util.concurrent.CountDownLatch(1);
            List<java.util.concurrent.Future<UUID>> tasks = new ArrayList<>();
            for (int i = 0; i < 8; i++) {
                List<ReserveStockCommandV1.Item> items = i % 2 == 0
                        ? List.of(new ReserveStockCommandV1.Item(first, 1), new ReserveStockCommandV1.Item(second, 1))
                        : List.of(new ReserveStockCommandV1.Item(second, 1), new ReserveStockCommandV1.Item(first, 1));
                var command = new ReserveStockCommandV1(UUID.randomUUID(), UUID.randomUUID(), Instant.now(), items);
                tasks.add(executor.submit(() -> { start.await(); handler.handle(command); return command.orderId(); }));
            }
            start.countDown();
            long successes = 0;
            for (var task : tasks) {
                UUID orderId = task.get(20, java.util.concurrent.TimeUnit.SECONDS);
                long reserved = count("stock_reservations", "order_id", orderId);
                assertThat(reserved).isIn(0L, 2L);
                if (reserved == 2) successes++;
            }
            assertThat(successes).isEqualTo(5);
        }
        assertStock(first, 0, 5);
        assertStock(second, 0, 5);
    }

    @Test
    void releasedReservationsProducePermanentConfirmationFailureWithoutStockMutation() {
        UUID first = stock(5), second = stock(6);
        var reserve = command(first, second);
        handler.handle(reserve);
        handler.handle(new ReleaseStockCommandV1(UUID.randomUUID(), reserve.orderId(), Instant.now()));
        var confirm = new ConfirmStockCommandV1(UUID.randomUUID(), reserve.orderId(), Instant.now());
        handler.handle(confirm);
        handler.handle(confirm);
        assertThat(event(reserve.orderId(), InventoryTopics.STOCK_CONFIRMATION_FAILED_V1, StockConfirmationFailedEventV1.class).reason())
                .isEqualTo("RESERVATIONS_RELEASED");
        assertStock(first, 5, 0);
        assertStock(second, 6, 0);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM outbox_messages WHERE aggregate_id = ? AND topic = ?", Long.class, reserve.orderId(), InventoryTopics.STOCK_CONFIRMED_V1)).isZero();
    }

    @Test
    void absentReservationsProducePermanentFailureAndReleaseIsAlreadySatisfied() {
        UUID orderId = UUID.randomUUID();
        handler.handle(new ConfirmStockCommandV1(UUID.randomUUID(), orderId, Instant.now()));
        assertThat(event(orderId, InventoryTopics.STOCK_CONFIRMATION_FAILED_V1, StockConfirmationFailedEventV1.class).reason())
                .isEqualTo("RESERVATIONS_NOT_FOUND");
        handler.handle(new ReleaseStockCommandV1(UUID.randomUUID(), orderId, Instant.now()));
        event(orderId, InventoryTopics.STOCK_RELEASED_V1, StockReleasedEventV1.class);
    }

    private UUID stock(int quantity) {
        UUID id = UUID.randomUUID();
        inventory.save(new InventoryItem(id, quantity, 0));
        return id;
    }

    private ReserveStockCommandV1 command(UUID first, UUID second) {
        return new ReserveStockCommandV1(UUID.randomUUID(), UUID.randomUUID(), Instant.now(), List.of(new ReserveStockCommandV1.Item(first, 2), new ReserveStockCommandV1.Item(second, 3)));
    }

    private void assertStock(UUID id, int available, int reserved) {
        InventoryItem item = inventory.findByProductId(id).orElseThrow();
        assertThat(item.getAvailableQuantity()).isEqualTo(available);
        assertThat(item.getReservedQuantity()).isEqualTo(reserved);
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
