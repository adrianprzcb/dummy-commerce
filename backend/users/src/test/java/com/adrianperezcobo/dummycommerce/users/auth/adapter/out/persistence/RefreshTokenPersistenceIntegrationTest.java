package com.adrianperezcobo.dummycommerce.users.auth.adapter.out.persistence;

import com.adrianperezcobo.dummycommerce.users.auth.application.port.out.RefreshTokenRepository;
import com.adrianperezcobo.dummycommerce.users.auth.domain.RefreshToken;
import com.adrianperezcobo.dummycommerce.users.user.adapter.out.persistence.UserJpaEntity;
import com.adrianperezcobo.dummycommerce.users.user.adapter.out.persistence.UserJpaRepository;
import com.adrianperezcobo.dummycommerce.users.user.domain.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.NONE;

@DataJpaTest(
        properties = "spring.jpa.hibernate.ddl-auto=validate"
)
@AutoConfigureTestDatabase(replace = NONE)
@Import(RefreshTokenPersistenceAdapter.class)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@Testcontainers
class RefreshTokenPersistenceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Test
    void shouldPersistAndLoadRefreshToken() {
        UUID userId = UUID.randomUUID();

        userJpaRepository.save(
                new UserJpaEntity(
                        userId,
                        "refresh@example.com",
                        "HASH",
                        Role.USER,
                        true,
                        Instant.now()
                )
        );

        RefreshToken token = new RefreshToken(
                UUID.randomUUID(),
                userId,
                "a".repeat(64),
                Instant.now().plusSeconds(3600),
                Instant.now(),
                null
        );

        refreshTokenRepository.save(token);

        RefreshToken loaded =
                refreshTokenRepository
                        .findByTokenHashForUpdate(
                                "a".repeat(64)
                        )
                        .orElseThrow();

        assertThat(loaded.getId())
                .isEqualTo(token.getId());

        assertThat(loaded.getUserId())
                .isEqualTo(userId);

        assertThat(loaded.isRevoked())
                .isFalse();
    }

    @Test
    void shouldPersistRevocation() {
        UUID userId = UUID.randomUUID();

        userJpaRepository.save(
                new UserJpaEntity(
                        userId,
                        "revoked@example.com",
                        "HASH",
                        Role.USER,
                        true,
                        Instant.now()
                )
        );

        RefreshToken token = new RefreshToken(
                UUID.randomUUID(),
                userId,
                "b".repeat(64),
                Instant.now().plusSeconds(3600),
                Instant.now(),
                null
        );

        refreshTokenRepository.save(token);

        token.revoke(Instant.now());

        refreshTokenRepository.save(token);

        RefreshToken loaded =
                refreshTokenRepository
                        .findByTokenHashForUpdate(
                                "b".repeat(64)
                        )
                        .orElseThrow();

        assertThat(loaded.isRevoked())
                .isTrue();

        assertThat(loaded.getRevokedAt())
                .isNotNull();
    }
}