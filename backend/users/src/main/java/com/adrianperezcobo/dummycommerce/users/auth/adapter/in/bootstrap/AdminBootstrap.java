package com.adrianperezcobo.dummycommerce.users.auth.adapter.in.bootstrap;

import com.adrianperezcobo.dummycommerce.users.auth.application.command.RegisterUserCommand;
import com.adrianperezcobo.dummycommerce.users.auth.application.port.in.RegisterUserUseCase;
import com.adrianperezcobo.dummycommerce.users.user.application.port.out.UserRepository;
import com.adrianperezcobo.dummycommerce.users.user.domain.Role;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.Locale;

@Component
@ConditionalOnProperty(name = "security.bootstrap-admin.enabled", havingValue = "true")
public class AdminBootstrap implements ApplicationRunner {
    private final RegisterUserUseCase registration;
    private final UserRepository users;
    private final String email;
    private final String password;

    public AdminBootstrap(RegisterUserUseCase registration, UserRepository users,
            @Value("${security.bootstrap-admin.email}") String email,
            @Value("${security.bootstrap-admin.password}") String password) {
        if (email.isBlank() || !email.contains("@") || email.length() > 320 || password.length() < 8 || password.length() > 72) {
            throw new IllegalArgumentException("Invalid bootstrap admin credentials");
        }
        this.registration = registration;
        this.users = users;
        this.email = email.trim().toLowerCase(Locale.ROOT);
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments arguments) {
        var existing = users.findByEmail(email);
        if (existing.isPresent()) {
            if (existing.get().getRole() != Role.ADMIN || !existing.get().isEnabled()) {
                throw new IllegalStateException("Bootstrap email belongs to an existing non-admin or disabled account");
            }
            return;
        }
        var user = registration.register(new RegisterUserCommand(email, password));
        user.changeRole(Role.ADMIN);
        users.save(user);
    }
}
