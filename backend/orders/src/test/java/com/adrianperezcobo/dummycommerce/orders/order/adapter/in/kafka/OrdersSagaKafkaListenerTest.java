package com.adrianperezcobo.dummycommerce.orders.order.adapter.in.kafka;

import com.adrianperezcobo.dummycommerce.orders.order.application.integration.inventory.*;
import com.adrianperezcobo.dummycommerce.orders.order.application.integration.payments.*;
import com.adrianperezcobo.dummycommerce.orders.order.application.service.OrderSagaHandler;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrdersSagaKafkaListenerTest {
    private final JsonMapper json = JsonMapper.builder().build();
    private final OrderSagaHandler handler = mock(OrderSagaHandler.class);
    private final OrdersSagaKafkaListener listener = new OrdersSagaKafkaListener(json, handler);

    @Test
    void deserializesStockReservedEventV1AndDelegates() {
        UUID orderId = UUID.randomUUID();
        var message = new StockReservedEventV1(UUID.randomUUID(), orderId, Instant.now());
        var record = new ConsumerRecord<String, String>(InventoryTopics.STOCK_RESERVED_V1, 0, 0, orderId.toString(), json.writeValueAsString(message));
        listener.onStockReservedEventV1(record);
        verify(handler).handle(message);
    }

    @Test
    void rejectsMismatchedKeyAndMalformedJson() {
        UUID orderId = UUID.randomUUID();
        var message = new StockReservedEventV1(UUID.randomUUID(), orderId, Instant.now());
        assertThatThrownBy(() -> listener.onStockReservedEventV1(new ConsumerRecord<>(InventoryTopics.STOCK_RESERVED_V1, 0, 0, "wrong-key", json.writeValueAsString(message))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> listener.onStockReservedEventV1(new ConsumerRecord<>(InventoryTopics.STOCK_RESERVED_V1, 0, 0, orderId.toString(), "invalid-json")))
                .isInstanceOf(RuntimeException.class);
        verifyNoInteractions(handler);
    }

    @Test
    void deserializesStockReservationFailedEventV1AndDelegates() {
        UUID orderId = UUID.randomUUID();
        var message = new StockReservationFailedEventV1(UUID.randomUUID(), orderId, Instant.now(), "failure");
        var record = new ConsumerRecord<String, String>(InventoryTopics.STOCK_RESERVATION_FAILED_V1, 0, 0, orderId.toString(), json.writeValueAsString(message));
        listener.onStockReservationFailedEventV1(record);
        verify(handler).handle(message);
    }

    @Test
    void deserializesPaymentCompletedEventV1AndDelegates() {
        UUID orderId = UUID.randomUUID();
        var message = new PaymentCompletedEventV1(UUID.randomUUID(), orderId, UUID.randomUUID(), Instant.now(), new BigDecimal("24.68"), "EUR");
        var record = new ConsumerRecord<String, String>(PaymentsTopics.PAYMENT_COMPLETED_V1, 0, 0, orderId.toString(), json.writeValueAsString(message));
        listener.onPaymentCompletedEventV1(record);
        verify(handler).handle(message);
    }

    @Test
    void deserializesPaymentFailedEventV1AndDelegates() {
        UUID orderId = UUID.randomUUID();
        var message = new PaymentFailedEventV1(UUID.randomUUID(), orderId, UUID.randomUUID(), Instant.now(), new BigDecimal("24.68"), "EUR", "failure");
        var record = new ConsumerRecord<String, String>(PaymentsTopics.PAYMENT_FAILED_V1, 0, 0, orderId.toString(), json.writeValueAsString(message));
        listener.onPaymentFailedEventV1(record);
        verify(handler).handle(message);
    }

    @Test
    void deserializesStockConfirmedEventV1AndDelegates() {
        UUID orderId = UUID.randomUUID();
        var message = new StockConfirmedEventV1(UUID.randomUUID(), orderId, Instant.now());
        var record = new ConsumerRecord<String, String>(InventoryTopics.STOCK_CONFIRMED_V1, 0, 0, orderId.toString(), json.writeValueAsString(message));
        listener.onStockConfirmedEventV1(record);
        verify(handler).handle(message);
    }

    @Test
    void deserializesStockReleasedEventV1AndDelegates() {
        UUID orderId = UUID.randomUUID();
        var message = new StockReleasedEventV1(UUID.randomUUID(), orderId, Instant.now());
        var record = new ConsumerRecord<String, String>(InventoryTopics.STOCK_RELEASED_V1, 0, 0, orderId.toString(), json.writeValueAsString(message));
        listener.onStockReleasedEventV1(record);
        verify(handler).handle(message);
    }

}
