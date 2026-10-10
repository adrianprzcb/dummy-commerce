package com.adrianperezcobo.dummycommerce.inventory.shared.inbox;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.*;
import java.time.Instant;
import java.util.UUID;

@Component
public class InboxAdapter implements InboxPort {
    private final InboxMessageJpaRepository repository;

    public InboxAdapter(InboxMessageJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean claim(UUID messageId, String topic) {
        return repository.insertIfAbsent(messageId, topic, Instant.now()) == 1;
    }
}
