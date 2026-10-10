package com.adrianperezcobo.dummycommerce.orders.order.adapter.in.kafka;

import com.adrianperezcobo.dummycommerce.orders.order.application.integration.inventory.*;
import com.adrianperezcobo.dummycommerce.orders.order.application.integration.payments.*;
import com.adrianperezcobo.dummycommerce.orders.order.application.integration.orders.*;
import com.adrianperezcobo.dummycommerce.orders.order.application.service.OrderSagaHandler;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;
import java.util.UUID;

@Component
public class OrdersSagaKafkaListener {
    private final JsonMapper jsonMapper;
    private final OrderSagaHandler handler;

    public OrdersSagaKafkaListener(JsonMapper jsonMapper, OrderSagaHandler handler) {
        this.jsonMapper = jsonMapper;
        this.handler = handler;
    }

    @KafkaListener(topics = InventoryTopics.STOCK_RESERVED_V1)
    public void onStockReservedEventV1(ConsumerRecord<String, String> record) {
        StockReservedEventV1 message = jsonMapper.readValue(record.value(), StockReservedEventV1.class);
        requireOrderKey(record.key(), message.orderId());
        handler.handle(message);
    }

    @KafkaListener(topics = InventoryTopics.STOCK_RESERVATION_FAILED_V1)
    public void onStockReservationFailedEventV1(ConsumerRecord<String, String> record) {
        StockReservationFailedEventV1 message = jsonMapper.readValue(record.value(), StockReservationFailedEventV1.class);
        requireOrderKey(record.key(), message.orderId());
        handler.handle(message);
    }

    @KafkaListener(topics = PaymentsTopics.PAYMENT_COMPLETED_V1)
    public void onPaymentCompletedEventV1(ConsumerRecord<String, String> record) {
        PaymentCompletedEventV1 message = jsonMapper.readValue(record.value(), PaymentCompletedEventV1.class);
        requireOrderKey(record.key(), message.orderId());
        handler.handle(message);
    }

    @KafkaListener(topics = PaymentsTopics.PAYMENT_FAILED_V1)
    public void onPaymentFailedEventV1(ConsumerRecord<String, String> record) {
        PaymentFailedEventV1 message = jsonMapper.readValue(record.value(), PaymentFailedEventV1.class);
        requireOrderKey(record.key(), message.orderId());
        handler.handle(message);
    }

    @KafkaListener(topics = InventoryTopics.STOCK_CONFIRMED_V1)
    public void onStockConfirmedEventV1(ConsumerRecord<String, String> record) {
        StockConfirmedEventV1 message = jsonMapper.readValue(record.value(), StockConfirmedEventV1.class);
        requireOrderKey(record.key(), message.orderId());
        handler.handle(message);
    }

    @KafkaListener(topics = InventoryTopics.STOCK_RELEASED_V1)
    public void onStockReleasedEventV1(ConsumerRecord<String, String> record) {
        StockReleasedEventV1 message = jsonMapper.readValue(record.value(), StockReleasedEventV1.class);
        requireOrderKey(record.key(), message.orderId());
        handler.handle(message);
    }

    @KafkaListener(topics = InventoryTopics.STOCK_CONFIRMATION_FAILED_V1)
    public void onStockConfirmationFailedEventV1(ConsumerRecord<String, String> record) {
        StockConfirmationFailedEventV1 message = jsonMapper.readValue(record.value(), StockConfirmationFailedEventV1.class);
        requireOrderKey(record.key(), message.orderId());
        handler.handle(message);
    }

    @KafkaListener(topics = PaymentsTopics.PAYMENT_REFUNDED_V1)
    public void onPaymentRefundedEventV1(ConsumerRecord<String, String> record) {
        PaymentRefundedEventV1 message = jsonMapper.readValue(record.value(), PaymentRefundedEventV1.class);
        requireOrderKey(record.key(), message.orderId());
        handler.handle(message);
    }

    @KafkaListener(topics = PaymentsTopics.PAYMENT_REFUND_FAILED_V1)
    public void onPaymentRefundFailedEventV1(ConsumerRecord<String, String> record) {
        PaymentRefundFailedEventV1 message = jsonMapper.readValue(record.value(), PaymentRefundFailedEventV1.class);
        requireOrderKey(record.key(), message.orderId());
        handler.handle(message);
    }

    private void requireOrderKey(String key, UUID orderId) {
        if (!orderId.toString().equals(key)) throw new IllegalArgumentException("Kafka key must match orderId");
    }
}
