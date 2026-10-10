package com.adrianperezcobo.dummycommerce.inventory.shared.outbox;

import org.junit.jupiter.api.*;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class OutboxPublisherTest {
    private final OutboxMessageJpaRepository repository = mock(OutboxMessageJpaRepository.class);
    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, String> kafka = mock(KafkaTemplate.class);
    private final OutboxPublisher publisher = new OutboxPublisher(repository, kafka, 1000, 0);
    private final OutboxMessageJpaEntity message = pending();

    @BeforeEach
    void setUp() {
        when(repository.findTop100ByStatusOrderByCreatedAtAscIdAsc(OutboxStatus.PENDING)).thenReturn(List.of(message));
    }

    @Test
    void marksPublishedOnlyAfterBrokerAcknowledgement() throws Exception {
        CompletableFuture<SendResult<String, String>> acknowledgement = new CompletableFuture<>();
        CountDownLatch sending = new CountDownLatch(1);
        when(kafka.send("topic", "order-key", "{}")) .thenAnswer(invocation -> {
            sending.countDown();
            return acknowledgement;
        });
        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            Future<?> execution = executor.submit(publisher::publishPending);
            assertThat(sending.await(1, TimeUnit.SECONDS)).isTrue();
            assertThat(message.getStatus()).isEqualTo(OutboxStatus.PENDING);
            verify(repository, never()).save(any());
            acknowledgement.complete(null);
            execution.get(2, TimeUnit.SECONDS);
        }
        assertThat(message.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(message.getPublishedAt()).isNotNull();
        verify(repository).save(message);
    }

    @Test
    void keepsPendingAndStopsBatchOnFailedSendThenRetries() {
        OutboxMessageJpaEntity later = pending();
        when(repository.findTop100ByStatusOrderByCreatedAtAscIdAsc(OutboxStatus.PENDING)).thenReturn(List.of(message, later));
        when(kafka.send("topic", "order-key", "{}"))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("broker down")))
                .thenReturn(CompletableFuture.completedFuture(null));
        publisher.publishPending();
        assertThat(message.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(message.getPublishedAt()).isNull();
        assertThat(later.getStatus()).isEqualTo(OutboxStatus.PENDING);
        verify(kafka, times(1)).send("topic", "order-key", "{}");
        verify(repository).save(message);
        assertThat(message.getAttemptCount()).isEqualTo(1);
        assertThat(message.getLastError()).contains("broker down");
        when(repository.findTop100ByStatusOrderByCreatedAtAscIdAsc(OutboxStatus.PENDING)).thenReturn(List.of(message));
        publisher.publishPending();
        assertThat(message.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
    }

    @Test
    void timeoutLeavesMessagePending() {
        when(kafka.send(anyString(), anyString(), anyString())).thenReturn(new CompletableFuture<>());
        new OutboxPublisher(repository, kafka, 1, 0).publishPending();
        assertThat(message.getStatus()).isEqualTo(OutboxStatus.PENDING);
        verify(repository).save(message);
        assertThat(message.getLastError()).isNotBlank();
    }

    @Test
    void synchronousSendFailureLeavesMessagePending() {
        when(kafka.send(anyString(), anyString(), anyString())).thenThrow(new IllegalStateException("send failed"));
        publisher.publishPending();
        assertThat(message.getStatus()).isEqualTo(OutboxStatus.PENDING);
        verify(repository).save(message);
        assertThat(message.getLastError()).isNotBlank();
    }

    @Test
    void failedPublicationWaitsForConfiguredRetryDelay() {
        when(kafka.send(anyString(), anyString(), anyString())).thenReturn(
                CompletableFuture.failedFuture(new IllegalStateException("broker down")));
        var delayed = new OutboxPublisher(repository, kafka, 1000, 60000);
        delayed.publishPending();
        delayed.publishPending();
        verify(kafka, times(1)).send(anyString(), anyString(), anyString());
        assertThat(message.getAttemptCount()).isEqualTo(1);
        assertThat(message.getLastAttemptAt()).isNotNull();
    }

    private static OutboxMessageJpaEntity pending() {
        return new OutboxMessageJpaEntity(UUID.randomUUID(), UUID.randomUUID(), "topic", "order-key", "{}", OutboxStatus.PENDING, Instant.now(), null);
    }
}
