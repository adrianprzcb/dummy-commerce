package com.adrianperezcobo.dummycommerce.users.shared.outbox;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.*;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.UUID;

@Component
public class OutboxAdapter
        implements OutboxPort {

    private final OutboxMessageJpaRepository repository;
    private final JsonMapper jsonMapper;

    public OutboxAdapter(
            OutboxMessageJpaRepository repository,
            JsonMapper jsonMapper
    ) {
        this.repository = repository;
        this.jsonMapper = jsonMapper;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void save(
            UUID messageId,
            UUID aggregateId,
            String topic,
            String messageKey,
            Object payload,
            Instant createdAt
    ) {
        String json = jsonMapper.writeValueAsString(
                payload
        );

        repository.save(
                new OutboxMessageJpaEntity(
                        messageId,
                        aggregateId,
                        topic,
                        messageKey,
                        json,
                        OutboxStatus.PENDING,
                        createdAt,
                        null
                )
        );
    }
}