package com.adrianperezcobo.dummycommerce.notifications.notification.domain;

import com.adrianperezcobo.dummycommerce.notifications.notification.domain.exception.InvalidNotificationStateException;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class Notification {

    private final UUID id;
    private final UUID sourceEventId;
    private final UUID userId;
    private final UUID orderId;
    private final NotificationChannel channel;
    private final String recipient;
    private final String subject;
    private final String message;
    private final Instant createdAt;

    private NotificationStatus status;
    private Instant sentAt;

    public Notification(
            UUID id,
            UUID sourceEventId,
            UUID userId,
            UUID orderId,
            NotificationChannel channel,
            String recipient,
            String subject,
            String message,
            NotificationStatus status,
            Instant createdAt,
            Instant sentAt
    ) {
        this.id = Objects.requireNonNull(id);
        this.sourceEventId =
                Objects.requireNonNull(sourceEventId);

        this.userId =
                Objects.requireNonNull(userId);

        this.orderId = orderId;

        this.channel =
                Objects.requireNonNull(channel);

        this.status =
                Objects.requireNonNull(status);

        this.createdAt =
                Objects.requireNonNull(createdAt);

        this.recipient = validateRecipient(
                recipient
        );

        this.subject = validateSubject(
                subject
        );

        this.message = validateMessage(
                message
        );

        validateSentAt(
                status,
                sentAt
        );

        this.sentAt = sentAt;
    }

    public void markSent(
            Instant sentAt
    ) {
        ensureStatus(
                NotificationStatus.PENDING
        );

        this.sentAt =
                Objects.requireNonNull(sentAt);

        this.status =
                NotificationStatus.SENT;
    }

    public void markFailed() {
        ensureStatus(
                NotificationStatus.PENDING
        );

        this.status =
                NotificationStatus.FAILED;

        this.sentAt = null;
    }

    private void ensureStatus(
            NotificationStatus expected
    ) {
        if (status != expected) {
            throw new InvalidNotificationStateException(
                    "Notification must be "
                            + expected
                            + " but is "
                            + status
            );
        }
    }

    private String validateRecipient(
            String recipient
    ) {
        if (recipient == null ||
                recipient.isBlank()) {

            throw new IllegalArgumentException(
                    "Notification recipient is required"
            );
        }

        String normalized =
                recipient.trim();

        if (normalized.length() > 320) {
            throw new IllegalArgumentException(
                    "Notification recipient must not exceed 320 characters"
            );
        }

        return normalized;
    }

    private String validateSubject(
            String subject
    ) {
        if (subject == null ||
                subject.isBlank()) {

            throw new IllegalArgumentException(
                    "Notification subject is required"
            );
        }

        String normalized =
                subject.trim();

        if (normalized.length() > 200) {
            throw new IllegalArgumentException(
                    "Notification subject must not exceed 200 characters"
            );
        }

        return normalized;
    }

    private String validateMessage(
            String message
    ) {
        if (message == null ||
                message.isBlank()) {

            throw new IllegalArgumentException(
                    "Notification message is required"
            );
        }

        return message.trim();
    }

    private void validateSentAt(
            NotificationStatus status,
            Instant sentAt
    ) {
        if (status == NotificationStatus.SENT &&
                sentAt == null) {

            throw new IllegalArgumentException(
                    "Sent notification requires sentAt"
            );
        }

        if (status != NotificationStatus.SENT &&
                sentAt != null) {

            throw new IllegalArgumentException(
                    "Only sent notifications can have sentAt"
            );
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getSourceEventId() {
        return sourceEventId;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public NotificationChannel getChannel() {
        return channel;
    }

    public String getRecipient() {
        return recipient;
    }

    public String getSubject() {
        return subject;
    }

    public String getMessage() {
        return message;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getSentAt() {
        return sentAt;
    }
}