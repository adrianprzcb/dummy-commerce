package com.adrianperezcobo.dummycommerce.inventory.reservation.adapter.in.kafka;

import com.adrianperezcobo.dummycommerce.inventory.reservation.application.integration.inventory.*;
import com.adrianperezcobo.dummycommerce.inventory.reservation.application.service.InventorySagaHandler;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class InventorySagaKafkaListenerTest {
    private final JsonMapper json = JsonMapper.builder().build();
    private final InventorySagaHandler handler = mock(InventorySagaHandler.class);
    private final InventorySagaKafkaListener listener = new InventorySagaKafkaListener(json, handler);

    @Test
    void deserializesReserveStockCommandV1AndDelegates() {
        UUID orderId = UUID.randomUUID();
        var message = new ReserveStockCommandV1(UUID.randomUUID(), orderId, Instant.now(), List.of(new ReserveStockCommandV1.Item(UUID.randomUUID(), 2)));
        var record = new ConsumerRecord<String, String>(InventoryTopics.RESERVE_STOCK_V1, 0, 0, orderId.toString(), json.writeValueAsString(message));
        listener.onReserveStockCommandV1(record);
        verify(handler).handle(message);
    }

    @Test
    void rejectsMismatchedKeyAndMalformedJson() {
        UUID orderId = UUID.randomUUID();
        var message = new ReserveStockCommandV1(UUID.randomUUID(), orderId, Instant.now(), List.of(new ReserveStockCommandV1.Item(UUID.randomUUID(), 2)));
        assertThatThrownBy(() -> listener.onReserveStockCommandV1(new ConsumerRecord<>(InventoryTopics.RESERVE_STOCK_V1, 0, 0, "wrong-key", json.writeValueAsString(message))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> listener.onReserveStockCommandV1(new ConsumerRecord<>(InventoryTopics.RESERVE_STOCK_V1, 0, 0, orderId.toString(), "invalid-json")))
                .isInstanceOf(RuntimeException.class);
        verifyNoInteractions(handler);
    }

    @Test
    void deserializesConfirmStockCommandV1AndDelegates() {
        UUID orderId = UUID.randomUUID();
        var message = new ConfirmStockCommandV1(UUID.randomUUID(), orderId, Instant.now());
        var record = new ConsumerRecord<String, String>(InventoryTopics.CONFIRM_STOCK_V1, 0, 0, orderId.toString(), json.writeValueAsString(message));
        listener.onConfirmStockCommandV1(record);
        verify(handler).handle(message);
    }

    @Test
    void deserializesReleaseStockCommandV1AndDelegates() {
        UUID orderId = UUID.randomUUID();
        var message = new ReleaseStockCommandV1(UUID.randomUUID(), orderId, Instant.now());
        var record = new ConsumerRecord<String, String>(InventoryTopics.RELEASE_STOCK_V1, 0, 0, orderId.toString(), json.writeValueAsString(message));
        listener.onReleaseStockCommandV1(record);
        verify(handler).handle(message);
    }

}
