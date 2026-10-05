package com.adrianperezcobo.dummycommerce.notifications.notification.adapter.out.persistence;

import com.adrianperezcobo.dummycommerce.notifications.notification.domain.NotificationChannel;
import com.adrianperezcobo.dummycommerce.notifications.notification.domain.NotificationStatus;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "notifications",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_notifications_source_event_id",
                columnNames = "source_event_id"
        )
)
public class NotificationJpaEntity {

    @Id
    private UUID id;

    @Column(
            name = "source_event_id",
            nullable = false
    )
    private UUID sourceEventId;

    @Column(
            name = "user_id",
            nullable = false
    )
    private UUID userId;

    @Column(
            name = "order_id"
    )
    private UUID orderId;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 30
    )
    private NotificationChannel channel;

    @Column(
            nullable = false,
            length = 320
    )
    private String recipient;

    @Column(
            nullable = false,
            length = 200
    )
    private String subject;

    @Column(
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 30
    )
    private NotificationStatus status;

    @Column(
            name = "created_at",
            nullable = false
    )
    private Instant createdAt;

    @Column(
            name = "sent_at"
    )
    private Instant sentAt;

    protected NotificationJpaEntity() {
    }

    public NotificationJpaEntity(
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
        this.id = id;
        this.sourceEventId = sourceEventId;
        this.userId = userId;
        this.orderId = orderId;
        this.channel = channel;
        this.recipient = recipient;
        this.subject = subject;
        this.message = message;
        this.status = status;
        this.createdAt = createdAt;
        this.sentAt = sentAt;
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