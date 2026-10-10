package com.adrianperezcobo.dummycommerce.users.auth.application.service;

import com.adrianperezcobo.dummycommerce.users.auth.application.command.RegisterUserCommand;
import com.adrianperezcobo.dummycommerce.users.shared.outbox.OutboxPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.AopTestUtils;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.json.JsonMapper;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
class UserOutboxIntegrationTest {
    @Container @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");
    @Autowired RegisterUserService service;
    @Autowired JdbcTemplate jdbc;
    @Autowired JsonMapper json;
    @MockitoSpyBean OutboxPort outbox;

    @Test
    void registrationPersistsNormalizedContactEventInSameTransaction() {
        var user = service.register(new RegisterUserCommand("User-" + UUID.randomUUID() + "@Example.com", "password123"));
        var row = jdbc.queryForMap("SELECT * FROM outbox_messages WHERE aggregate_id = ?", user.getId());
        assertThat(row.get("status")).isEqualTo("PENDING");
        assertThat(row.get("message_key")).isEqualTo(user.getId().toString());
        var event = json.readTree((String) row.get("payload"));
        assertThat(event.get("email").asText()).isEqualTo(user.getEmail());
        assertThat(event.get("messageId").asText()).isEqualTo(row.get("id").toString());
    }

    @Test
    void outboxFailureRollsBackRegistration() {
        OutboxPort target = AopTestUtils.getUltimateTargetObject(outbox);
        doThrow(new IllegalStateException("outbox unavailable")).when(target).save(any(), any(), any(), any(), any(), any());
        String email = UUID.randomUUID() + "@example.com";
        assertThatThrownBy(() -> service.register(new RegisterUserCommand(email, "password123"))).isInstanceOf(IllegalStateException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE email = ?", Long.class, email)).isZero();
    }
    @Test
    void upgradePublishesExistingContactOnceWithoutChangingUser() {
        var legacy = org.flywaydb.core.Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .schemas("legacy_users").defaultSchema("legacy_users").target("4").load();
        legacy.migrate();
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO legacy_users.users(id, email, password_hash, role, enabled, created_at) VALUES (?, 'legacy@example.com', 'HASH', 'USER', true, now())", id);
        var upgrade = org.flywaydb.core.Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .schemas("legacy_users").defaultSchema("legacy_users").load();
        upgrade.migrate();
        upgrade.migrate();
        var row = jdbc.queryForMap("SELECT * FROM legacy_users.outbox_messages WHERE aggregate_id = ?", id);
        assertThat(row.get("status")).isEqualTo("PENDING");
        assertThat(row.get("message_key")).isEqualTo(id.toString());
        var event = json.readTree((String) row.get("payload"));
        assertThat(event.get("messageId").asText()).isEqualTo(row.get("id").toString());
        assertThat(event.get("email").asText()).isEqualTo("legacy@example.com");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM legacy_users.outbox_messages", Long.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT email FROM legacy_users.users WHERE id = ?", String.class, id)).isEqualTo("legacy@example.com");
    }
}

