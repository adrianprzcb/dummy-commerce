package com.adrianperezcobo.dummycommerce.users.auth.application.service;

import com.adrianperezcobo.dummycommerce.users.auth.application.port.out.RefreshTokenPort;
import com.adrianperezcobo.dummycommerce.users.auth.application.port.out.RefreshTokenRepository;
import com.adrianperezcobo.dummycommerce.users.auth.domain.RefreshToken;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LogoutServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private RefreshTokenPort refreshTokenPort;

    @InjectMocks
    private LogoutService service;

    @Test
    void shouldRevokeRefreshToken() {
        RefreshToken token = new RefreshToken(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "HASH",
                Instant.now().plusSeconds(3600),
                Instant.now(),
                null
        );

        when(refreshTokenPort.hash("RAW_TOKEN"))
                .thenReturn("HASH");

        when(refreshTokenRepository
                .findByTokenHashForUpdate("HASH"))
                .thenReturn(Optional.of(token));

        service.logout("RAW_TOKEN");

        assertThat(token.isRevoked()).isTrue();

        verify(refreshTokenRepository)
                .save(token);
    }

    @Test
    void shouldDoNothingForUnknownToken() {
        when(refreshTokenPort.hash("UNKNOWN"))
                .thenReturn("HASH");

        when(refreshTokenRepository
                .findByTokenHashForUpdate("HASH"))
                .thenReturn(Optional.empty());

        service.logout("UNKNOWN");

        verify(refreshTokenRepository, never())
                .save(any());
    }
}