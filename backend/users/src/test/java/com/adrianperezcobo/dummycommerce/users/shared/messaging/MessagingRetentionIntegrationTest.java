package com.adrianperezcobo.dummycommerce.users.shared.messaging;

import com.adrianperezcobo.dummycommerce.users.shared.outbox.OutboxMessageJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.postgresql.PostgreSQLContainer;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@Testcontainers
class MessagingRetentionIntegrationTest {
    @Container @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");
    @Autowired JdbcTemplate jdbc;
    @Autowired OutboxMessageJpaRepository outbox;

    @Test
    void cleansOnlyOldPublishedMessages() {
        UUID old = outbox("PUBLISHED", Instant.now().minus(31, ChronoUnit.DAYS));
        UUID recent = outbox("PUBLISHED", Instant.now().minus(1, ChronoUnit.DAYS));
        UUID pending = outbox("PENDING", Instant.now().minus(100, ChronoUnit.DAYS));
        assertThat(outbox.deletePublishedBefore(Instant.now().minus(30, ChronoUnit.DAYS), 1000)).isEqualTo(1);
        assertThat(jdbc.queryForList("SELECT id FROM outbox_messages", UUID.class)).contains(recent, pending).doesNotContain(old);
    }

    @Test
    void cleanupBatchIsBounded() {
        outbox("PUBLISHED", Instant.now().minus(31, ChronoUnit.DAYS));
        outbox("PUBLISHED", Instant.now().minus(31, ChronoUnit.DAYS));
        assertThat(outbox.deletePublishedBefore(Instant.now().minus(30, ChronoUnit.DAYS), 1)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM outbox_messages", Long.class)).isEqualTo(1);
    }

    private UUID outbox(String status, Instant publishedAt) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO outbox_messages(id, aggregate_id, topic, message_key, payload, status, created_at, published_at) VALUES (?, ?, 'test', 'key', '{}', ?, ?, ?)",
                id, UUID.randomUUID(), status, java.sql.Timestamp.from(publishedAt),
                status.equals("PENDING") ? null : java.sql.Timestamp.from(publishedAt));
        return id;
    }

}
