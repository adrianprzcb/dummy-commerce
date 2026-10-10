package com.adrianperezcobo.dummycommerce.notifications.shared.messaging;

import com.adrianperezcobo.dummycommerce.notifications.shared.inbox.InboxMessageJpaRepository;
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
    @Autowired InboxMessageJpaRepository inbox;

    @Test
    void cleansOnlyInboxOlderThanConservativeRetentionWindow() {
        UUID old = UUID.randomUUID(), recent = UUID.randomUUID();
        jdbc.update("INSERT INTO inbox_messages(message_id, topic, processed_at) VALUES (?, 'test', ?)",
                old, java.sql.Timestamp.from(Instant.now().minus(91, ChronoUnit.DAYS)));
        jdbc.update("INSERT INTO inbox_messages(message_id, topic, processed_at) VALUES (?, 'test', ?)",
                recent, java.sql.Timestamp.from(Instant.now().minus(89, ChronoUnit.DAYS)));
        assertThat(inbox.deleteProcessedBefore(Instant.now().minus(90, ChronoUnit.DAYS), 1000)).isEqualTo(1);
        assertThat(jdbc.queryForList("SELECT message_id FROM inbox_messages", UUID.class)).contains(recent).doesNotContain(old);
    }
}
