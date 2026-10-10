package com.adrianperezcobo.dummycommerce.users.auth.adapter.in.bootstrap;

import com.adrianperezcobo.dummycommerce.users.auth.application.command.RegisterUserCommand;
import com.adrianperezcobo.dummycommerce.users.auth.application.port.in.RegisterUserUseCase;
import com.adrianperezcobo.dummycommerce.users.auth.application.port.out.PasswordEncoderPort;
import com.adrianperezcobo.dummycommerce.users.user.application.port.out.UserRepository;
import com.adrianperezcobo.dummycommerce.users.user.domain.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "security.bootstrap-customer.enabled=true",
        "security.bootstrap-customer.email=clienteprueba@example.com",
        "security.bootstrap-customer.password=ClientePrueba!!23",
        "outbox.publisher.enabled=false"})
@Testcontainers
class CustomerBootstrapIntegrationTest {
    @Container @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");
    @Autowired CustomerBootstrap bootstrap;
    @Autowired UserRepository users;
    @Autowired RegisterUserUseCase registration;
    @Autowired PasswordEncoderPort passwords;
    @Autowired JdbcTemplate jdbc;

    @Test
    void startupCreatesCustomerAndContactEventOnlyOnce() {
        var customer = users.findByEmail("clienteprueba@example.com").orElseThrow();
        assertThat(customer.getRole()).isEqualTo(Role.USER);
        assertThat(customer.isEnabled()).isTrue();
        assertThat(customer.getPasswordHash()).isNotEqualTo("ClientePrueba!!23");
        assertThat(passwords.matches("ClientePrueba!!23", customer.getPasswordHash())).isTrue();

        bootstrap.run(new DefaultApplicationArguments());

        assertThat(users.findByEmail(customer.getEmail()).orElseThrow().getId()).isEqualTo(customer.getId());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM users WHERE email=?", Integer.class, customer.getEmail())).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_messages WHERE aggregate_id=?", Integer.class, customer.getId())).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT topic FROM outbox_messages WHERE aggregate_id=?", String.class, customer.getId()))
                .isEqualTo("dummy-commerce.users.user-registered.v1");
    }

    @Test
    void restartPreservesAnExistingCustomersPassword() {
        var customer = registration.register(new RegisterUserCommand(UUID.randomUUID() + "@example.com", "ExistingPassword123!"));
        new CustomerBootstrap(registration, users, customer.getEmail(), "DifferentPassword123!").run(new DefaultApplicationArguments());

        var saved = users.findByEmail(customer.getEmail()).orElseThrow();
        assertThat(saved.getPasswordHash()).isEqualTo(customer.getPasswordHash());
        assertThat(passwords.matches("ExistingPassword123!", saved.getPasswordHash())).isTrue();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_messages WHERE aggregate_id=?", Integer.class, saved.getId())).isEqualTo(1);
    }

    @Test
    void existingAdminIsNeverConvertedIntoCustomer() {
        var admin = registration.register(new RegisterUserCommand(UUID.randomUUID() + "@example.com", "ExistingAdmin123!"));
        admin.changeRole(Role.ADMIN);
        users.save(admin);

        var attempt = new CustomerBootstrap(registration, users, admin.getEmail(), "ClientePrueba!!23");
        assertThatThrownBy(() -> attempt.run(new DefaultApplicationArguments())).isInstanceOf(IllegalStateException.class);
        assertThat(users.findById(admin.getId()).orElseThrow().getRole()).isEqualTo(Role.ADMIN);
    }
}
