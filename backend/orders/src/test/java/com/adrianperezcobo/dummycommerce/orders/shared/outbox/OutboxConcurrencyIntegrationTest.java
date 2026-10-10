package com.adrianperezcobo.dummycommerce.orders.shared.outbox;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.postgresql.PostgreSQLContainer;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
class OutboxConcurrencyIntegrationTest {
    @Container @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");
    @Autowired OutboxMessageJpaRepository repository;
    @Autowired PlatformTransactionManager transactions;
    @Autowired JdbcTemplate jdbc;

    @Test
    @Timeout(30)
    @SuppressWarnings("unchecked")
    void simultaneousPublishersDoNotPublishCommittedMessagesTwice() throws Exception {
        for (int i = 0; i < 5; i++) repository.save(new OutboxMessageJpaEntity(UUID.randomUUID(), UUID.randomUUID(), "test-topic", "key", "{}", OutboxStatus.PENDING, Instant.now(), null));
        KafkaTemplate<String, String> kafka = mock(KafkaTemplate.class);
        when(kafka.send(anyString(), anyString(), anyString())).thenReturn(CompletableFuture.completedFuture(null));
        var publisher = new OutboxPublisher(repository, kafka, 1000, 0);
        var transaction = new TransactionTemplate(transactions);
        try (var executor = Executors.newFixedThreadPool(3)) {
            CountDownLatch start = new CountDownLatch(1);
            List<Future<?>> tasks = new ArrayList<>();
            for (int i = 0; i < 3; i++) tasks.add(executor.submit(() -> {
                start.await();
                transaction.executeWithoutResult(status -> publisher.publishPending());
                return null;
            }));
            start.countDown();
            for (var task : tasks) task.get(20, TimeUnit.SECONDS);
        }
        verify(kafka, times(5)).send(anyString(), anyString(), anyString());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM outbox_messages WHERE status = 'PUBLISHED'", Long.class)).isEqualTo(5);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM outbox_messages WHERE attempt_count = 1", Long.class)).isEqualTo(5);
    }
}
