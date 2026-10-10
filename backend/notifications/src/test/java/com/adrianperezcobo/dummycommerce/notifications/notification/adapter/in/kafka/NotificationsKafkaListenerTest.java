package com.adrianperezcobo.dummycommerce.notifications.notification.adapter.in.kafka;

import com.adrianperezcobo.dummycommerce.notifications.notification.application.service.NotificationEventHandler;
import com.adrianperezcobo.dummycommerce.notifications.notification.application.integration.orders.*;
import com.adrianperezcobo.dummycommerce.notifications.user.application.integration.users.*;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import java.time.Instant;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class NotificationsKafkaListenerTest {
    private final JsonMapper json = JsonMapper.builder().build();
    private final NotificationEventHandler handler = mock(NotificationEventHandler.class);
    private final NotificationsKafkaListener listener = new NotificationsKafkaListener(json, handler);

    @Test
    void routesRegistrationToProjectionHandler() {
        var event = new UserRegisteredEventV1(UUID.randomUUID(), UUID.randomUUID(), "buyer@example.com", Instant.now());
        listener.onUserRegistered(new ConsumerRecord<>(UsersTopics.USER_REGISTERED_V1, 0, 0, event.userId().toString(), json.writeValueAsString(event)));
        verify(handler).handle(event);
    }

    @Test
    void routesBothOrderOutcomesToNotificationHandler() {
        var confirmed = new OrderConfirmedEventV1(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), Instant.now());
        var cancelled = new OrderCancelledEventV1(UUID.randomUUID(), UUID.randomUUID(), confirmed.userId(), Instant.now(), "PAYMENT_FAILED");
        listener.onOrderConfirmed(new ConsumerRecord<>(OrdersTopics.ORDER_CONFIRMED_V1, 0, 0, confirmed.orderId().toString(), json.writeValueAsString(confirmed)));
        listener.onOrderCancelled(new ConsumerRecord<>(OrdersTopics.ORDER_CANCELLED_V1, 0, 0, cancelled.orderId().toString(), json.writeValueAsString(cancelled)));
        verify(handler).handle(confirmed);
        verify(handler).handle(cancelled);
    }

    @Test
    void malformedPayloadOrKeyDoesNotReachBusinessHandler() {
        var event = new UserRegisteredEventV1(UUID.randomUUID(), UUID.randomUUID(), "buyer@example.com", Instant.now());
        assertThatThrownBy(() -> listener.onUserRegistered(new ConsumerRecord<>(UsersTopics.USER_REGISTERED_V1, 0, 0, "wrong", json.writeValueAsString(event)))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> listener.onOrderConfirmed(new ConsumerRecord<>(OrdersTopics.ORDER_CONFIRMED_V1, 0, 0, "key", "invalid-json"))).isInstanceOf(RuntimeException.class);
        verifyNoInteractions(handler);
    }
}
