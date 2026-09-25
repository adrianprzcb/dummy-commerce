package com.adrianperezcobo.dummycommerce.users.shared.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.NONE;

@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@Testcontainers
class UserDatabaseConstraintIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer(
                    "postgres:17-alpine"
            );

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldRejectDuplicatedEmail() {
        UUID firstId = UUID.randomUUID();
        UUID secondId = UUID.randomUUID();

        insertUser(
                firstId,
                "duplicate@example.com"
        );

        assertThatThrownBy(() ->
                insertUser(
                        secondId,
                        "duplicate@example.com"
                )
        ).isInstanceOf(
                DataIntegrityViolationException.class
        );
    }

    @Test
    void shouldRejectInvalidRole() {
        assertThatThrownBy(() ->
                jdbcTemplate.update(
                        """
                        INSERT INTO users (
                            id,
                            email,
                            password_hash,
                            role,
                            enabled,
                            created_at
                        )
                        VALUES (?, ?, ?, ?, ?, ?)
                        """,
                        UUID.randomUUID(),
                        "role@example.com",
                        "HASH",
                        "SUPER_ADMIN",
                        true,
                        now()
                )
        ).isInstanceOf(
                DataIntegrityViolationException.class
        );
    }

    @Test
    void shouldRejectDuplicatedRefreshTokenHash() {
        UUID userId = UUID.randomUUID();

        insertUser(
                userId,
                "refresh@example.com"
        );

        String hash = "a".repeat(64);

        insertRefreshToken(
                UUID.randomUUID(),
                userId,
                hash
        );

        assertThatThrownBy(() ->
                insertRefreshToken(
                        UUID.randomUUID(),
                        userId,
                        hash
                )
        ).isInstanceOf(
                DataIntegrityViolationException.class
        );
    }

    private void insertUser(
            UUID id,
            String email
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO users (
                    id,
                    email,
                    password_hash,
                    role,
                    enabled,
                    created_at
                )
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                id,
                email,
                "HASH",
                "USER",
                true,
                now()
        );
    }

    private void insertRefreshToken(
            UUID id,
            UUID userId,
            String tokenHash
    ) {
        OffsetDateTime now = now();

        jdbcTemplate.update(
                """
                INSERT INTO refresh_tokens (
                    id,
                    user_id,
                    token_hash,
                    expires_at,
                    created_at,
                    revoked_at
                )
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                id,
                userId,
                tokenHash,
                now.plusHours(1),
                now,
                null
        );
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }
}