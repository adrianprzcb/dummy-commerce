package com.adrianperezcobo.dummycommerce.users.auth.adapter.in.bootstrap;

import com.adrianperezcobo.dummycommerce.users.user.application.port.out.UserRepository;
import com.adrianperezcobo.dummycommerce.users.user.domain.Role;
import com.adrianperezcobo.dummycommerce.users.auth.application.port.in.RegisterUserUseCase;
import com.adrianperezcobo.dummycommerce.users.auth.application.command.RegisterUserCommand;
import com.adrianperezcobo.dummycommerce.users.auth.application.port.out.PasswordEncoderPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.postgresql.PostgreSQLContainer;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "security.bootstrap-admin.enabled=true",
        "security.bootstrap-admin.email=bootstrap@example.com",
        "security.bootstrap-admin.password=BootstrapTest123!"})
@Testcontainers
class AdminBootstrapIntegrationTest {
    @Container @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");
    @Autowired AdminBootstrap bootstrap;
    @Autowired UserRepository users;
    @Autowired PasswordEncoderPort passwords;
    @Autowired RegisterUserUseCase registration;
    @Autowired JdbcTemplate jdbc;

    @Test
    void startupCreatesAdminWithHashedPasswordAndContactOutboxAndDoesNotDuplicateOnRestart() {
        var admin = users.findByEmail("bootstrap@example.com").orElseThrow();
        assertThat(admin.getRole()).isEqualTo(Role.ADMIN);
        assertThat(passwords.matches("BootstrapTest123!", admin.getPasswordHash())).isTrue();
        bootstrap.run(new DefaultApplicationArguments());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_messages WHERE aggregate_id=?", Integer.class, admin.getId())).isEqualTo(1);
    }

    @Test
    void existingNormalAccountIsNeverSilentlyPromoted() {
        var user = registration.register(new RegisterUserCommand("existing@example.com", "ExistingTest123!"));
        var attempt = new AdminBootstrap(registration, users, user.getEmail(), "BootstrapTest123!");
        assertThatThrownBy(() -> attempt.run(new DefaultApplicationArguments())).isInstanceOf(IllegalStateException.class);
        assertThat(users.findById(user.getId()).orElseThrow().getRole()).isEqualTo(Role.USER);
    }

    @Test
    void enablingBootstrapWithMissingCredentialsFails() {
        assertThatThrownBy(() -> new AdminBootstrap(registration, users, "", "")).isInstanceOf(IllegalArgumentException.class);
    }
}
