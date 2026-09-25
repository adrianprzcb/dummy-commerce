package com.adrianperezcobo.dummycommerce.users.auth.application.service;

import com.adrianperezcobo.dummycommerce.users.auth.application.command.LoginUserCommand;
import com.adrianperezcobo.dummycommerce.users.auth.application.exception.InvalidCredentialsException;
import com.adrianperezcobo.dummycommerce.users.auth.application.exception.UserDisabledException;
import com.adrianperezcobo.dummycommerce.users.auth.application.port.out.AccessTokenPort;
import com.adrianperezcobo.dummycommerce.users.auth.application.port.out.PasswordEncoderPort;
import com.adrianperezcobo.dummycommerce.users.auth.application.port.out.RefreshTokenPort;
import com.adrianperezcobo.dummycommerce.users.auth.application.port.out.RefreshTokenRepository;
import com.adrianperezcobo.dummycommerce.users.auth.application.result.LoginResult;
import com.adrianperezcobo.dummycommerce.users.user.application.port.out.UserRepository;
import com.adrianperezcobo.dummycommerce.users.user.domain.Role;
import com.adrianperezcobo.dummycommerce.users.user.domain.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoderPort passwordEncoder;

    @Mock
    private AccessTokenPort accessTokenPort;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private RefreshTokenPort refreshTokenPort;

    @InjectMocks
    private LoginService service;

    @Test
    void shouldLoginAndGenerateTokens() {
        User user = createUser(true);

        when(userRepository.findByEmail(user.getEmail()))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "password123",
                user.getPasswordHash()
        )).thenReturn(true);

        when(accessTokenPort.generate(user))
                .thenReturn("ACCESS_TOKEN");

        when(accessTokenPort.getExpirationSeconds())
                .thenReturn(900L);

        when(refreshTokenPort.generate())
                .thenReturn("REFRESH_TOKEN");

        when(refreshTokenPort.hash("REFRESH_TOKEN"))
                .thenReturn("REFRESH_HASH");

        when(refreshTokenPort.getExpirationSeconds())
                .thenReturn(2_592_000L);

        when(refreshTokenRepository.save(any()))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        LoginResult result = service.login(
                new LoginUserCommand(
                        user.getEmail(),
                        "password123"
                )
        );

        assertThat(result.accessToken())
                .isEqualTo("ACCESS_TOKEN");

        assertThat(result.refreshToken())
                .isEqualTo("REFRESH_TOKEN");

        assertThat(result.accessTokenExpiresIn())
                .isEqualTo(900L);

        assertThat(result.refreshTokenExpiresIn())
                .isEqualTo(2_592_000L);

        verify(refreshTokenRepository)
                .save(any());
    }

    @Test
    void shouldRejectWrongPassword() {
        User user = createUser(true);

        when(userRepository.findByEmail(user.getEmail()))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "wrong-password",
                user.getPasswordHash()
        )).thenReturn(false);

        assertThatThrownBy(() ->
                service.login(
                        new LoginUserCommand(
                                user.getEmail(),
                                "wrong-password"
                        )
                )
        ).isInstanceOf(
                InvalidCredentialsException.class
        );

        verifyNoInteractions(accessTokenPort);
        verifyNoInteractions(refreshTokenRepository);
    }

    @Test
    void shouldRejectDisabledUser() {
        User user = createUser(false);

        when(userRepository.findByEmail(user.getEmail()))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "password123",
                user.getPasswordHash()
        )).thenReturn(true);

        assertThatThrownBy(() ->
                service.login(
                        new LoginUserCommand(
                                user.getEmail(),
                                "password123"
                        )
                )
        ).isInstanceOf(UserDisabledException.class);

        verifyNoInteractions(accessTokenPort);
    }

    private User createUser(boolean enabled) {
        return new User(
                UUID.randomUUID(),
                "adrian@example.com",
                "HASHED_PASSWORD",
                Role.USER,
                enabled,
                Instant.now()
        );
    }
}