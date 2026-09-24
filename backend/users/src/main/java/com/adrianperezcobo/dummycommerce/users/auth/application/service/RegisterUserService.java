package com.adrianperezcobo.dummycommerce.users.auth.application.service;

import com.adrianperezcobo.dummycommerce.users.auth.application.command.RegisterUserCommand;
import com.adrianperezcobo.dummycommerce.users.auth.application.exception.EmailAlreadyExistsException;
import com.adrianperezcobo.dummycommerce.users.auth.application.port.in.RegisterUserUseCase;
import com.adrianperezcobo.dummycommerce.users.auth.application.port.out.PasswordEncoderPort;
import com.adrianperezcobo.dummycommerce.users.user.application.port.out.UserRepository;
import com.adrianperezcobo.dummycommerce.users.user.domain.Role;
import com.adrianperezcobo.dummycommerce.users.user.domain.User;
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

    public RegisterUserService(
            UserRepository userRepository,
            PasswordEncoderPort passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
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

        return userRepository.save(user);
    }

    private String normalizeEmail(String email) {
        return email
                .trim()
                .toLowerCase(Locale.ROOT);
    }
}