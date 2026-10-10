package com.adrianperezcobo.dummycommerce.inventory.reservation.adapter.in.kafka;

import com.adrianperezcobo.dummycommerce.inventory.reservation.application.integration.inventory.*;
import com.adrianperezcobo.dummycommerce.inventory.reservation.application.service.InventorySagaHandler;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;
import java.util.UUID;

@Component
public class InventorySagaKafkaListener {
    private final JsonMapper jsonMapper;
    private final InventorySagaHandler handler;

    public InventorySagaKafkaListener(JsonMapper jsonMapper, InventorySagaHandler handler) {
        this.jsonMapper = jsonMapper;
        this.handler = handler;
    }

    @KafkaListener(topics = InventoryTopics.RESERVE_STOCK_V1)
    public void onReserveStockCommandV1(ConsumerRecord<String, String> record) {
        ReserveStockCommandV1 message = jsonMapper.readValue(record.value(), ReserveStockCommandV1.class);
        requireOrderKey(record.key(), message.orderId());
        handler.handle(message);
    }

    @KafkaListener(topics = InventoryTopics.CONFIRM_STOCK_V1)
    public void onConfirmStockCommandV1(ConsumerRecord<String, String> record) {
        ConfirmStockCommandV1 message = jsonMapper.readValue(record.value(), ConfirmStockCommandV1.class);
        requireOrderKey(record.key(), message.orderId());
        handler.handle(message);
    }

    @KafkaListener(topics = InventoryTopics.RELEASE_STOCK_V1)
    public void onReleaseStockCommandV1(ConsumerRecord<String, String> record) {
        ReleaseStockCommandV1 message = jsonMapper.readValue(record.value(), ReleaseStockCommandV1.class);
        requireOrderKey(record.key(), message.orderId());
        handler.handle(message);
    }

    private void requireOrderKey(String key, UUID orderId) {
        if (!orderId.toString().equals(key)) throw new IllegalArgumentException("Kafka key must match orderId");
    }
}
