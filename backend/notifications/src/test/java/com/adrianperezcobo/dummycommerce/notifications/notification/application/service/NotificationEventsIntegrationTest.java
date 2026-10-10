package com.adrianperezcobo.dummycommerce.notifications.notification.application.service;

import com.adrianperezcobo.dummycommerce.notifications.notification.application.integration.orders.*;
import com.adrianperezcobo.dummycommerce.notifications.notification.application.exception.UserContactNotAvailableException;
import com.adrianperezcobo.dummycommerce.notifications.notification.application.port.out.*;
import com.adrianperezcobo.dummycommerce.notifications.user.application.integration.users.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.postgresql.PostgreSQLContainer;
import java.time.Instant;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
class NotificationEventsIntegrationTest {
    @Container @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");
    @Autowired NotificationEventHandler handler;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean NotificationSenderPort sender;

    @Test
    void projectionAndFinalNotificationAreIdempotent() {
        UUID userId = UUID.randomUUID();
        var registered = new UserRegisteredEventV1(UUID.randomUUID(), userId, "buyer@example.com", Instant.now());
        handler.handle(registered);
        handler.handle(registered);
        var confirmed = new OrderConfirmedEventV1(UUID.randomUUID(), UUID.randomUUID(), userId, Instant.now());
        when(sender.send(any(), any(), any(), any(), any())).thenReturn(NotificationSenderResult.SUCCESS);
        handler.handle(confirmed);
        handler.handle(confirmed);
        var row = jdbc.queryForMap("SELECT * FROM notifications WHERE source_event_id = ?", confirmed.messageId());
        assertThat(row.get("recipient")).isEqualTo("buyer@example.com");
        assertThat(row.get("status")).isEqualTo("SENT");
        assertThat(row.get("subject")).isEqualTo("Order confirmed");
        verify(sender, times(1)).send(any(), any(), any(), any(), any());
    }

    @Test
    void missingContactRollsBackInboxAndCanRecoverOnRedelivery() {
        UUID userId = UUID.randomUUID();
        var cancelled = new OrderCancelledEventV1(UUID.randomUUID(), UUID.randomUUID(), userId, Instant.now(), "PAYMENT_FAILED");
        assertThatThrownBy(() -> handler.handle(cancelled)).isInstanceOf(UserContactNotAvailableException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM inbox_messages WHERE message_id = ?", Long.class, cancelled.messageId())).isZero();
        verifyNoInteractions(sender);
        handler.handle(new UserRegisteredEventV1(UUID.randomUUID(), userId, "buyer@example.com", Instant.now()));
        when(sender.send(any(), any(), any(), any(), any())).thenReturn(NotificationSenderResult.SUCCESS);
        handler.handle(cancelled);
        handler.handle(cancelled);
        assertThat(jdbc.queryForObject("SELECT status FROM notifications WHERE source_event_id = ?", String.class, cancelled.messageId())).isEqualTo("SENT");
        verify(sender, times(1)).send(any(), any(), any(), any(), any());
    }

    @Test
    void olderContactEventDoesNotReplaceNewerProjection() {
        UUID userId = UUID.randomUUID();
        handler.handle(new UserRegisteredEventV1(UUID.randomUUID(), userId, "current@example.com", Instant.now()));
        handler.handle(new UserRegisteredEventV1(UUID.randomUUID(), userId, "old@example.com", Instant.now().minusSeconds(60)));
        assertThat(jdbc.queryForObject("SELECT email FROM user_contacts WHERE user_id = ?", String.class, userId)).isEqualTo("current@example.com");
    }

    @Test
    void senderTechnicalFailureRollsBackNotificationAndInbox() {
        UUID userId = UUID.randomUUID();
        handler.handle(new UserRegisteredEventV1(UUID.randomUUID(), userId, "buyer@example.com", Instant.now()));
        var event = new OrderConfirmedEventV1(UUID.randomUUID(), UUID.randomUUID(), userId, Instant.now());
        when(sender.send(any(), any(), any(), any(), any())).thenThrow(new IllegalStateException("sender unavailable"));
        assertThatThrownBy(() -> handler.handle(event)).isInstanceOf(IllegalStateException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM notifications WHERE source_event_id = ?", Long.class, event.messageId())).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM inbox_messages WHERE message_id = ?", Long.class, event.messageId())).isZero();
        doReturn(NotificationSenderResult.SUCCESS).when(sender).send(any(), any(), any(), any(), any());
        handler.handle(event);
        assertThat(jdbc.queryForObject("SELECT status FROM notifications WHERE source_event_id = ?", String.class, event.messageId())).isEqualTo("SENT");
    }
}
