package com.adrianperezcobo.dummycommerce.inventory.shared.messaging;

import com.adrianperezcobo.dummycommerce.inventory.shared.outbox.OutboxMessageJpaRepository;
import com.adrianperezcobo.dummycommerce.inventory.shared.inbox.InboxMessageJpaRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
@ConditionalOnProperty(name = "messaging.retention.enabled", havingValue = "true", matchIfMissing = true)
public class MessagingRetention {
    private final OutboxMessageJpaRepository outbox;
    private final InboxMessageJpaRepository inbox;
    private final long outboxDays;
    private final long inboxDays;
    private final int batchSize;

    public MessagingRetention(OutboxMessageJpaRepository outbox, InboxMessageJpaRepository inbox, 
            @Value("${messaging.retention.outbox-days:30}") long outboxDays,
            @Value("${messaging.retention.inbox-days:90}") long inboxDays,
            @Value("${messaging.retention.batch-size:1000}") int batchSize) {
        if (outboxDays < 1 || inboxDays < 1 || batchSize < 1) throw new IllegalArgumentException("Invalid messaging retention policy");
        this.outbox = outbox;
        this.inbox = inbox;
        this.outboxDays = outboxDays;
        this.inboxDays = inboxDays;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${messaging.retention.fixed-delay-ms:43200000}")
    @Transactional
    public void clean() {
        Instant now = Instant.now();
        outbox.deletePublishedBefore(now.minus(outboxDays, ChronoUnit.DAYS), batchSize);
        inbox.deleteProcessedBefore(now.minus(inboxDays, ChronoUnit.DAYS), batchSize);
    }
}
