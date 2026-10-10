package com.adrianperezcobo.dummycommerce.payments.shared.outbox;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_messages")
public class OutboxMessageJpaEntity {

    @Id
    private UUID id;

    @Column(
            name = "aggregate_id",
            nullable = false
    )
    private UUID aggregateId;

    @Column(
            nullable = false,
            length = 255
    )
    private String topic;

    @Column(
            name = "message_key",
            nullable = false,
            length = 255
    )
    private String messageKey;

    @Column(
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 30
    )
    private OutboxStatus status;

    @Column(
            name = "created_at",
            nullable = false
    )
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;
    @Column(name = "last_error", length = 2000)
    private String lastError;
    @Column(name = "last_attempt_at")
    private Instant lastAttemptAt;

    public void recordAttempt(Instant now) {
        attemptCount++;
        lastAttemptAt = now;
    }

    public void recordFailure(Throwable failure) {
        String error = failure.getClass().getSimpleName() + ": " + failure.getMessage();
        lastError = error.substring(0, Math.min(2000, error.length()));
    }

    public int getAttemptCount() { return attemptCount; }
    public String getLastError() { return lastError; }
    public Instant getLastAttemptAt() { return lastAttemptAt; }

    protected OutboxMessageJpaEntity() {
    }

    public OutboxMessageJpaEntity(
            UUID id,
            UUID aggregateId,
            String topic,
            String messageKey,
            String payload,
            OutboxStatus status,
            Instant createdAt,
            Instant publishedAt
    ) {
        this.id = id;
        this.aggregateId = aggregateId;
        this.topic = topic;
        this.messageKey = messageKey;
        this.payload = payload;
        this.status = status;
        this.createdAt = createdAt;
        this.publishedAt = publishedAt;
    }

    public void markPublished(Instant publishedAt) {
        this.status = OutboxStatus.PUBLISHED;
        this.lastError = null;
        this.publishedAt = publishedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getAggregateId() {
        return aggregateId;
    }

    public String getTopic() {
        return topic;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public String getPayload() {
        return payload;
    }

    public OutboxStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }
}
