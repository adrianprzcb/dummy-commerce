package com.adrianperezcobo.dummycommerce.payments.shared.inbox;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inbox_messages")
public class InboxMessageJpaEntity {
    @Id
    @Column(name = "message_id")
    private UUID messageId;
    @Column(nullable = false, length = 255)
    private String topic;
    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    protected InboxMessageJpaEntity() { }
}
