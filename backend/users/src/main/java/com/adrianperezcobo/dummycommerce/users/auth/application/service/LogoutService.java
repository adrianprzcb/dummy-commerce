package com.adrianperezcobo.dummycommerce.users.auth.application.service;

import com.adrianperezcobo.dummycommerce.users.auth.application.port.in.LogoutUseCase;
import com.adrianperezcobo.dummycommerce.users.auth.application.port.out.RefreshTokenPort;
import com.adrianperezcobo.dummycommerce.users.auth.application.port.out.RefreshTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@Transactional
public class LogoutService implements LogoutUseCase {

    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenPort refreshTokenPort;

    public LogoutService(
            RefreshTokenRepository refreshTokenRepository,
            RefreshTokenPort refreshTokenPort
    ) {
        this.refreshTokenRepository =
                refreshTokenRepository;

        this.refreshTokenPort =
                refreshTokenPort;
    }

    @Override
    public void logout(String rawRefreshToken) {

        String hash =
                refreshTokenPort.hash(
                        rawRefreshToken
                );

        refreshTokenRepository
                .findByTokenHashForUpdate(hash)
                .ifPresent(token -> {

                    token.revoke(Instant.now());

                    refreshTokenRepository.save(token);
                });
    }
}