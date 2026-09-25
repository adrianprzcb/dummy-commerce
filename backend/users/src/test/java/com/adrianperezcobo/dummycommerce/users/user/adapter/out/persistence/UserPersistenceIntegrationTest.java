package com.adrianperezcobo.dummycommerce.users.user.adapter.out.persistence;

import com.adrianperezcobo.dummycommerce.users.user.application.port.out.UserRepository;
import com.adrianperezcobo.dummycommerce.users.user.domain.Role;
import com.adrianperezcobo.dummycommerce.users.user.domain.User;
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
@Import(UserPersistenceAdapter.class)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@Testcontainers
class UserPersistenceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldPersistAndLoadUser() {
        User user = new User(
                UUID.randomUUID(),
                "Adrian@Example.com",
                "HASHED_PASSWORD",
                Role.USER,
                true,
                Instant.now()
        );

        userRepository.save(user);

        User loaded = userRepository
                .findById(user.getId())
                .orElseThrow();

        assertThat(loaded.getId())
                .isEqualTo(user.getId());

        assertThat(loaded.getEmail())
                .isEqualTo("adrian@example.com");

        assertThat(loaded.getPasswordHash())
                .isEqualTo("HASHED_PASSWORD");

        assertThat(loaded.getRole())
                .isEqualTo(Role.USER);

        assertThat(loaded.isEnabled())
                .isTrue();
    }

    @Test
    void shouldFindUserByEmail() {
        User user = new User(
                UUID.randomUUID(),
                "test@example.com",
                "HASH",
                Role.USER,
                true,
                Instant.now()
        );

        userRepository.save(user);

        User loaded = userRepository
                .findByEmail("test@example.com")
                .orElseThrow();

        assertThat(loaded.getId())
                .isEqualTo(user.getId());
    }

    @Test
    void shouldDetectExistingEmail() {
        User user = new User(
                UUID.randomUUID(),
                "existing@example.com",
                "HASH",
                Role.USER,
                true,
                Instant.now()
        );

        userRepository.save(user);

        assertThat(
                userRepository.existsByEmail(
                        "existing@example.com"
                )
        ).isTrue();
    }
}