package com.adrianperezcobo.dummycommerce.users.auth.application.service;

import com.adrianperezcobo.dummycommerce.users.auth.application.command.RefreshTokenCommand;
import com.adrianperezcobo.dummycommerce.users.auth.application.exception.InvalidRefreshTokenException;
import com.adrianperezcobo.dummycommerce.users.auth.application.exception.UserDisabledException;
import com.adrianperezcobo.dummycommerce.users.auth.application.port.in.RefreshTokenUseCase;
import com.adrianperezcobo.dummycommerce.users.auth.application.port.out.AccessTokenPort;
import com.adrianperezcobo.dummycommerce.users.auth.application.port.out.RefreshTokenPort;
import com.adrianperezcobo.dummycommerce.users.auth.application.port.out.RefreshTokenRepository;
import com.adrianperezcobo.dummycommerce.users.auth.application.result.RefreshTokenResult;
import com.adrianperezcobo.dummycommerce.users.auth.domain.RefreshToken;
import com.adrianperezcobo.dummycommerce.users.user.application.port.out.UserRepository;
import com.adrianperezcobo.dummycommerce.users.user.domain.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Transactional
public class RefreshTokenService
        implements RefreshTokenUseCase {

    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenPort refreshTokenPort;
    private final UserRepository userRepository;
    private final AccessTokenPort accessTokenPort;

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            RefreshTokenPort refreshTokenPort,
            UserRepository userRepository,
            AccessTokenPort accessTokenPort
    ) {
        this.refreshTokenRepository =
                refreshTokenRepository;

        this.refreshTokenPort =
                refreshTokenPort;

        this.userRepository =
                userRepository;

        this.accessTokenPort =
                accessTokenPort;
    }

    @Override
    public RefreshTokenResult refresh(
            RefreshTokenCommand command
    ) {

        String tokenHash =
                refreshTokenPort.hash(
                        command.refreshToken()
                );

        RefreshToken existing =
                refreshTokenRepository
                        .findByTokenHashForUpdate(
                                tokenHash
                        )
                        .orElseThrow(
                                InvalidRefreshTokenException::new
                        );

        Instant now = Instant.now();

        if (
                existing.isRevoked() ||
                        existing.isExpired(now)
        ) {
            throw new InvalidRefreshTokenException();
        }

        User user = userRepository
                .findById(existing.getUserId())
                .orElseThrow(
                        InvalidRefreshTokenException::new
                );

        if (!user.isEnabled()) {
            throw new UserDisabledException();
        }

        // Rotación: el token antiguo deja de ser válido.
        existing.revoke(now);
        refreshTokenRepository.save(existing);

        String newRawRefreshToken =
                refreshTokenPort.generate();

        RefreshToken newRefreshToken =
                new RefreshToken(
                        UUID.randomUUID(),
                        user.getId(),
                        refreshTokenPort.hash(
                                newRawRefreshToken
                        ),
                        now.plusSeconds(
                                refreshTokenPort
                                        .getExpirationSeconds()
                        ),
                        now,
                        null
                );

        refreshTokenRepository.save(
                newRefreshToken
        );

        String accessToken =
                accessTokenPort.generate(user);

        return new RefreshTokenResult(
                accessToken,
                newRawRefreshToken,
                accessTokenPort.getExpirationSeconds(),
                refreshTokenPort.getExpirationSeconds()
        );
    }
}