package com.adrianperezcobo.dummycommerce.users.auth.application.service;

import com.adrianperezcobo.dummycommerce.users.auth.application.command.RefreshTokenCommand;
import com.adrianperezcobo.dummycommerce.users.auth.application.exception.InvalidRefreshTokenException;
import com.adrianperezcobo.dummycommerce.users.auth.application.port.out.AccessTokenPort;
import com.adrianperezcobo.dummycommerce.users.auth.application.port.out.RefreshTokenPort;
import com.adrianperezcobo.dummycommerce.users.auth.application.port.out.RefreshTokenRepository;
import com.adrianperezcobo.dummycommerce.users.auth.application.result.RefreshTokenResult;
import com.adrianperezcobo.dummycommerce.users.auth.domain.RefreshToken;
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
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private RefreshTokenPort refreshTokenPort;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccessTokenPort accessTokenPort;

    @InjectMocks
    private RefreshTokenService service;

    @Test
    void shouldRotateRefreshToken() {
        User user = createUser();

        RefreshToken existing = new RefreshToken(
                UUID.randomUUID(),
                user.getId(),
                "OLD_HASH",
                Instant.now().plusSeconds(3600),
                Instant.now(),
                null
        );

        when(refreshTokenPort.hash("OLD_TOKEN"))
                .thenReturn("OLD_HASH");

        when(refreshTokenRepository
                .findByTokenHashForUpdate("OLD_HASH"))
                .thenReturn(Optional.of(existing));

        when(userRepository.findById(user.getId()))
                .thenReturn(Optional.of(user));

        when(refreshTokenPort.generate())
                .thenReturn("NEW_TOKEN");

        when(refreshTokenPort.hash("NEW_TOKEN"))
                .thenReturn("NEW_HASH");

        when(refreshTokenPort.getExpirationSeconds())
                .thenReturn(2_592_000L);

        when(accessTokenPort.generate(user))
                .thenReturn("NEW_ACCESS_TOKEN");

        when(accessTokenPort.getExpirationSeconds())
                .thenReturn(900L);

        when(refreshTokenRepository.save(any()))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        RefreshTokenResult result =
                service.refresh(
                        new RefreshTokenCommand(
                                "OLD_TOKEN"
                        )
                );

        assertThat(existing.isRevoked()).isTrue();

        assertThat(result.accessToken())
                .isEqualTo("NEW_ACCESS_TOKEN");

        assertThat(result.refreshToken())
                .isEqualTo("NEW_TOKEN");

        verify(refreshTokenRepository, times(2))
                .save(any());
    }

    @Test
    void shouldRejectUnknownRefreshToken() {
        when(refreshTokenPort.hash("INVALID"))
                .thenReturn("INVALID_HASH");

        when(refreshTokenRepository
                .findByTokenHashForUpdate(
                        "INVALID_HASH"
                ))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.refresh(
                        new RefreshTokenCommand(
                                "INVALID"
                        )
                )
        ).isInstanceOf(
                InvalidRefreshTokenException.class
        );
    }

    @Test
    void shouldRejectRevokedRefreshToken() {
        User user = createUser();

        RefreshToken token = new RefreshToken(
                UUID.randomUUID(),
                user.getId(),
                "HASH",
                Instant.now().plusSeconds(3600),
                Instant.now(),
                Instant.now()
        );

        when(refreshTokenPort.hash("TOKEN"))
                .thenReturn("HASH");

        when(refreshTokenRepository
                .findByTokenHashForUpdate("HASH"))
                .thenReturn(Optional.of(token));

        assertThatThrownBy(() ->
                service.refresh(
                        new RefreshTokenCommand("TOKEN")
                )
        ).isInstanceOf(
                InvalidRefreshTokenException.class
        );

        verifyNoInteractions(userRepository);
    }

    private User createUser() {
        return new User(
                UUID.randomUUID(),
                "adrian@example.com",
                "HASH",
                Role.USER,
                true,
                Instant.now()
        );
    }
}