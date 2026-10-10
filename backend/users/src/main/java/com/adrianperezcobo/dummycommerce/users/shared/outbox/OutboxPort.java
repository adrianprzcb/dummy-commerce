package com.adrianperezcobo.dummycommerce.users.shared.outbox;

import java.time.Instant;
import java.util.UUID;

public interface OutboxPort {

    void save(
            UUID messageId,
            UUID aggregateId,
            String topic,
            String messageKey,
            Object payload,
            Instant createdAt
    );
}