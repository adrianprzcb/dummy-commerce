package com.adrianperezcobo.dummycommerce.notifications.notification.domain;

import com.adrianperezcobo.dummycommerce.notifications.notification.domain.exception.InvalidNotificationStateException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotificationTest {

    @Test
    void shouldCreatePendingNotification() {
        Notification notification =
                notification(
                        NotificationStatus.PENDING,
                        null
                );

        assertThat(notification.getId())
                .isNotNull();

        assertThat(notification.getStatus())
                .isEqualTo(NotificationStatus.PENDING);

        assertThat(notification.getSentAt())
                .isNull();
    }

    @Test
    void shouldNormalizeTextValues() {
        Instant now = Instant.now();

        Notification notification =
                new Notification(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        NotificationChannel.EMAIL,
                        "  test@example.com  ",
                        "  Order confirmed  ",
                        "  Your order is ready.  ",
                        NotificationStatus.PENDING,
                        now,
                        null
                );

        assertThat(notification.getRecipient())
                .isEqualTo("test@example.com");

        assertThat(notification.getSubject())
                .isEqualTo("Order confirmed");

        assertThat(notification.getMessage())
                .isEqualTo("Your order is ready.");
    }

    @Test
    void shouldAllowNotificationWithoutOrderId() {
        Instant now = Instant.now();

        Notification notification =
                new Notification(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        null,
                        NotificationChannel.EMAIL,
                        "test@example.com",
                        "Welcome",
                        "Welcome to Dummy Commerce",
                        NotificationStatus.PENDING,
                        now,
                        null
                );

        assertThat(notification.getOrderId())
                .isNull();
    }

    @Test
    void shouldMarkPendingNotificationAsSent() {
        Notification notification =
                notification(
                        NotificationStatus.PENDING,
                        null
                );

        Instant sentAt = Instant.now();

        notification.markSent(sentAt);

        assertThat(notification.getStatus())
                .isEqualTo(NotificationStatus.SENT);

        assertThat(notification.getSentAt())
                .isEqualTo(sentAt);
    }

    @Test
    void shouldMarkPendingNotificationAsFailed() {
        Notification notification =
                notification(
                        NotificationStatus.PENDING,
                        null
                );

        notification.markFailed();

        assertThat(notification.getStatus())
                .isEqualTo(NotificationStatus.FAILED);

        assertThat(notification.getSentAt())
                .isNull();
    }

    @Test
    void shouldRejectMarkingSentNotificationAsSentAgain() {
        Notification notification =
                notification(
                        NotificationStatus.SENT,
                        Instant.now()
                );

        assertThatThrownBy(() ->
                notification.markSent(
                        Instant.now()
                )
        ).isInstanceOf(
                InvalidNotificationStateException.class
        );
    }

    @Test
    void shouldRejectMarkingFailedNotificationAsSent() {
        Notification notification =
                notification(
                        NotificationStatus.FAILED,
                        null
                );

        assertThatThrownBy(() ->
                notification.markSent(
                        Instant.now()
                )
        ).isInstanceOf(
                InvalidNotificationStateException.class
        );
    }

    @Test
    void shouldRejectMarkingSentNotificationAsFailed() {
        Notification notification =
                notification(
                        NotificationStatus.SENT,
                        Instant.now()
                );

        assertThatThrownBy(
                notification::markFailed
        ).isInstanceOf(
                InvalidNotificationStateException.class
        );
    }

    @Test
    void shouldRejectSentNotificationWithoutSentAt() {
        assertThatThrownBy(() ->
                notification(
                        NotificationStatus.SENT,
                        null
                )
        ).isInstanceOf(
                IllegalArgumentException.class
        );
    }

    @Test
    void shouldRejectPendingNotificationWithSentAt() {
        assertThatThrownBy(() ->
                notification(
                        NotificationStatus.PENDING,
                        Instant.now()
                )
        ).isInstanceOf(
                IllegalArgumentException.class
        );
    }

    @Test
    void shouldRejectFailedNotificationWithSentAt() {
        assertThatThrownBy(() ->
                notification(
                        NotificationStatus.FAILED,
                        Instant.now()
                )
        ).isInstanceOf(
                IllegalArgumentException.class
        );
    }

    @Test
    void shouldRejectBlankRecipient() {
        assertThatThrownBy(() ->
                createWith(
                        " ",
                        "Subject",
                        "Message"
                )
        ).isInstanceOf(
                IllegalArgumentException.class
        );
    }

    @Test
    void shouldRejectBlankSubject() {
        assertThatThrownBy(() ->
                createWith(
                        "test@example.com",
                        " ",
                        "Message"
                )
        ).isInstanceOf(
                IllegalArgumentException.class
        );
    }

    @Test
    void shouldRejectBlankMessage() {
        assertThatThrownBy(() ->
                createWith(
                        "test@example.com",
                        "Subject",
                        " "
                )
        ).isInstanceOf(
                IllegalArgumentException.class
        );
    }

    private Notification notification(
            NotificationStatus status,
            Instant sentAt
    ) {
        return new Notification(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                NotificationChannel.EMAIL,
                "test@example.com",
                "Order confirmed",
                "Your order has been confirmed.",
                status,
                Instant.now(),
                sentAt
        );
    }

    private void createWith(
            String recipient,
            String subject,
            String message
    ) {
        new Notification(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                NotificationChannel.EMAIL,
                recipient,
                subject,
                message,
                NotificationStatus.PENDING,
                Instant.now(),
                null
        );
    }
}