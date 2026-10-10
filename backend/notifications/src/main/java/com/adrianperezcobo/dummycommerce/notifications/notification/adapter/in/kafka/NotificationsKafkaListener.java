package com.adrianperezcobo.dummycommerce.notifications.notification.adapter.in.kafka;

import com.adrianperezcobo.dummycommerce.notifications.notification.application.integration.orders.*;
import com.adrianperezcobo.dummycommerce.notifications.notification.application.service.NotificationEventHandler;
import com.adrianperezcobo.dummycommerce.notifications.user.application.integration.users.*;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;
import java.util.UUID;

@Component
public class NotificationsKafkaListener {
    private final JsonMapper json;
    private final NotificationEventHandler handler;

    public NotificationsKafkaListener(JsonMapper json, NotificationEventHandler handler) {
        this.json = json;
        this.handler = handler;
    }

    @KafkaListener(topics = UsersTopics.USER_REGISTERED_V1)
    public void onUserRegistered(ConsumerRecord<String, String> record) {
        UserRegisteredEventV1 event = json.readValue(record.value(), UserRegisteredEventV1.class);
        requireKey(record.key(), event.userId());
        handler.handle(event);
    }

    @KafkaListener(topics = OrdersTopics.ORDER_CONFIRMED_V1)
    public void onOrderConfirmed(ConsumerRecord<String, String> record) {
        OrderConfirmedEventV1 event = json.readValue(record.value(), OrderConfirmedEventV1.class);
        requireKey(record.key(), event.orderId());
        handler.handle(event);
    }

    @KafkaListener(topics = OrdersTopics.ORDER_CANCELLED_V1)
    public void onOrderCancelled(ConsumerRecord<String, String> record) {
        OrderCancelledEventV1 event = json.readValue(record.value(), OrderCancelledEventV1.class);
        requireKey(record.key(), event.orderId());
        handler.handle(event);
    }

    private void requireKey(String key, UUID id) {
        if (!id.toString().equals(key)) throw new IllegalArgumentException("Kafka key must match aggregate id");
    }
}
