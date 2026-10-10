package com.adrianperezcobo.dummycommerce.inventory.shared.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.concurrent.*;

@Component
@ConditionalOnProperty(name = "outbox.publisher.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxPublisher {
    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);
    private final OutboxMessageJpaRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final long sendTimeoutMs;
    private final long retryDelayMs;

    public OutboxPublisher(OutboxMessageJpaRepository repository, KafkaTemplate<String, String> kafkaTemplate,
                           @Value("${outbox.publisher.send-timeout-ms:10000}") long sendTimeoutMs,
                           @Value("${outbox.publisher.retry-delay-ms:5000}") long retryDelayMs) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
        this.sendTimeoutMs = sendTimeoutMs;
        this.retryDelayMs = retryDelayMs;
        if (sendTimeoutMs <= 0 || retryDelayMs < 0) throw new IllegalArgumentException("Invalid Outbox publisher timing");
    }

    @Scheduled(fixedDelayString = "${outbox.publisher.fixed-delay-ms:1000}")
    @Transactional
    public void publishPending() {
        // Keep row locks until commit so parallel publishers preserve publication order.
        for (OutboxMessageJpaEntity message : repository.findTop100ByStatusOrderByCreatedAtAscIdAsc(OutboxStatus.PENDING)) {
            Instant now = Instant.now();
            if (message.getLastError() != null && message.getLastAttemptAt() != null
                    && now.isBefore(message.getLastAttemptAt().plusMillis(retryDelayMs))) return;
            message.recordAttempt(now);
            try {
                kafkaTemplate.send(message.getTopic(), message.getMessageKey(), message.getPayload())
                        .get(sendTimeoutMs, TimeUnit.MILLISECONDS);
            } catch (InterruptedException ex) {
                message.recordFailure(ex);
                repository.save(message);
                Thread.currentThread().interrupt();
                log.warn("Outbox publication interrupted for {}", message.getId(), ex);
                return;
            } catch (ExecutionException | TimeoutException | RuntimeException ex) {
                message.recordFailure(ex.getCause() == null ? ex : ex.getCause());
                repository.save(message);
                log.warn("Outbox publication failed for {}; message remains PENDING", message.getId(), ex);
                // Stop this batch: do not publish a later command ahead of a failed one.
                return;
            }
            message.markPublished(Instant.now());
            repository.save(message);
        }
    }
}
