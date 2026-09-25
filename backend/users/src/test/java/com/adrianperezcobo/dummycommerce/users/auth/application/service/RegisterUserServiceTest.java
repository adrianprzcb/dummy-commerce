package com.adrianperezcobo.dummycommerce.users.auth.application.service;

import com.adrianperezcobo.dummycommerce.users.auth.application.command.RegisterUserCommand;
import com.adrianperezcobo.dummycommerce.users.auth.application.exception.EmailAlreadyExistsException;
import com.adrianperezcobo.dummycommerce.users.auth.application.port.out.PasswordEncoderPort;
import com.adrianperezcobo.dummycommerce.users.user.application.port.out.UserRepository;
import com.adrianperezcobo.dummycommerce.users.user.domain.Role;
import com.adrianperezcobo.dummycommerce.users.user.domain.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegisterUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoderPort passwordEncoder;

    @InjectMocks
    private RegisterUserService service;

    @Test
    void shouldRegisterUser() {
        RegisterUserCommand command =
                new RegisterUserCommand(
                        "Adrian@Example.com",
                        "password123"
                );

        when(userRepository.existsByEmail(
                "adrian@example.com"
        )).thenReturn(false);

        when(passwordEncoder.encode("password123"))
                .thenReturn("HASHED_PASSWORD");

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        User result = service.register(command);

        assertThat(result.getId()).isNotNull();

        assertThat(result.getEmail())
                .isEqualTo("adrian@example.com");

        assertThat(result.getPasswordHash())
                .isEqualTo("HASHED_PASSWORD");

        assertThat(result.getRole())
                .isEqualTo(Role.USER);

        assertThat(result.isEnabled()).isTrue();
        assertThat(result.getCreatedAt()).isNotNull();

        verify(passwordEncoder)
                .encode("password123");

        verify(userRepository)
                .save(any(User.class));
    }

    @Test
    void shouldRejectDuplicatedEmail() {
        RegisterUserCommand command =
                new RegisterUserCommand(
                        "adrian@example.com",
                        "password123"
                );

        when(userRepository.existsByEmail(
                "adrian@example.com"
        )).thenReturn(true);

        assertThatThrownBy(
                () -> service.register(command)
        )
                .isInstanceOf(
                        EmailAlreadyExistsException.class
                );

        verifyNoInteractions(passwordEncoder);

        verify(userRepository, never())
                .save(any());
    }
}