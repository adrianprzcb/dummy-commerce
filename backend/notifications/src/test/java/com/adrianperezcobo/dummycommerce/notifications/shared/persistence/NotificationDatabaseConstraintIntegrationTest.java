package com.adrianperezcobo.dummycommerce.notifications.shared.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.NONE;

@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@Testcontainers
class NotificationDatabaseConstraintIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer(
                    "postgres:17-alpine"
            );

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldRejectDuplicateSourceEventId() {
        UUID sourceEventId =
                UUID.randomUUID();

        insert(
                UUID.randomUUID(),
                sourceEventId,
                "EMAIL",
                "recipient@example.com",
                "Subject",
                "Message",
                "PENDING",
                null
        );

        assertThatThrownBy(() ->
                insert(
                        UUID.randomUUID(),
                        sourceEventId,
                        "EMAIL",
                        "recipient@example.com",
                        "Subject",
                        "Message",
                        "PENDING",
                        null
                )
        ).isInstanceOf(
                DataIntegrityViolationException.class
        );
    }

    @Test
    void shouldRejectInvalidChannel() {
        assertThatThrownBy(() ->
                insert(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "SMS",
                        "recipient",
                        "Subject",
                        "Message",
                        "PENDING",
                        null
                )
        ).isInstanceOf(
                DataIntegrityViolationException.class
        );
    }

    @Test
    void shouldRejectInvalidStatus() {
        assertThatThrownBy(() ->
                insert(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "EMAIL",
                        "test@example.com",
                        "Subject",
                        "Message",
                        "UNKNOWN",
                        null
                )
        ).isInstanceOf(
                DataIntegrityViolationException.class
        );
    }

    @Test
    void shouldRejectBlankRecipient() {
        assertThatThrownBy(() ->
                insert(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "EMAIL",
                        "   ",
                        "Subject",
                        "Message",
                        "PENDING",
                        null
                )
        ).isInstanceOf(
                DataIntegrityViolationException.class
        );
    }

    @Test
    void shouldRejectBlankSubject() {
        assertThatThrownBy(() ->
                insert(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "EMAIL",
                        "test@example.com",
                        " ",
                        "Message",
                        "PENDING",
                        null
                )
        ).isInstanceOf(
                DataIntegrityViolationException.class
        );
    }

    @Test
    void shouldRejectBlankMessage() {
        assertThatThrownBy(() ->
                insert(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "EMAIL",
                        "test@example.com",
                        "Subject",
                        " ",
                        "PENDING",
                        null
                )
        ).isInstanceOf(
                DataIntegrityViolationException.class
        );
    }

    @Test
    void shouldRejectSentNotificationWithoutSentAt() {
        assertThatThrownBy(() ->
                insert(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "EMAIL",
                        "test@example.com",
                        "Subject",
                        "Message",
                        "SENT",
                        null
                )
        ).isInstanceOf(
                DataIntegrityViolationException.class
        );
    }

    @Test
    void shouldRejectPendingNotificationWithSentAt() {
        assertThatThrownBy(() ->
                insert(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "EMAIL",
                        "test@example.com",
                        "Subject",
                        "Message",
                        "PENDING",
                        now()
                )
        ).isInstanceOf(
                DataIntegrityViolationException.class
        );
    }

    private void insert(
            UUID id,
            UUID sourceEventId,
            String channel,
            String recipient,
            String subject,
            String message,
            String status,
            OffsetDateTime sentAt
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO notifications (
                    id,
                    source_event_id,
                    user_id,
                    order_id,
                    channel,
                    recipient,
                    subject,
                    message,
                    status,
                    created_at,
                    sent_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                id,
                sourceEventId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                channel,
                recipient,
                subject,
                message,
                status,
                now(),
                sentAt
        );
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(
                ZoneOffset.UTC
        );
    }
}