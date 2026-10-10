package com.adrianperezcobo.dummycommerce.users.auth.application.service;

import com.adrianperezcobo.dummycommerce.users.auth.application.command.RegisterUserCommand;
import com.adrianperezcobo.dummycommerce.users.auth.application.exception.EmailAlreadyExistsException;
import com.adrianperezcobo.dummycommerce.users.auth.application.port.in.RegisterUserUseCase;
import com.adrianperezcobo.dummycommerce.users.auth.application.port.out.PasswordEncoderPort;
import com.adrianperezcobo.dummycommerce.users.user.application.port.out.UserRepository;
import com.adrianperezcobo.dummycommerce.users.user.domain.Role;
import com.adrianperezcobo.dummycommerce.users.user.domain.User;
import com.adrianperezcobo.dummycommerce.users.shared.outbox.OutboxPort;
import com.adrianperezcobo.dummycommerce.users.user.application.integration.users.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Service
@Transactional
public class RegisterUserService
        implements RegisterUserUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoderPort passwordEncoder;
    private final OutboxPort outbox;

    public RegisterUserService(
            UserRepository userRepository,
            PasswordEncoderPort passwordEncoder,
            OutboxPort outbox
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.outbox = outbox;
    }

    @Override
    public User register(RegisterUserCommand command) {

        String normalizedEmail =
                normalizeEmail(command.email());

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyExistsException(
                    normalizedEmail
            );
        }

        String passwordHash =
                passwordEncoder.encode(command.password());

        User user = new User(
                UUID.randomUUID(),
                normalizedEmail,
                passwordHash,
                Role.USER,
                true,
                Instant.now()
        );

        User saved = userRepository.save(user);
        UUID messageId = UUID.randomUUID();
        Instant now = Instant.now();
        outbox.save(messageId, saved.getId(), UsersTopics.USER_REGISTERED_V1, saved.getId().toString(),
                new UserRegisteredEventV1(messageId, saved.getId(), saved.getEmail(), now), now);
        return saved;
    }

    private String normalizeEmail(String email) {
        return email
                .trim()
                .toLowerCase(Locale.ROOT);
    }
}