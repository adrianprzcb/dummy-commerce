package com.adrianperezcobo.dummycommerce.users.auth.application.service;

import com.adrianperezcobo.dummycommerce.users.auth.application.command.LoginUserCommand;
import com.adrianperezcobo.dummycommerce.users.auth.application.exception.InvalidCredentialsException;
import com.adrianperezcobo.dummycommerce.users.auth.application.exception.UserDisabledException;
import com.adrianperezcobo.dummycommerce.users.auth.application.port.in.LoginUseCase;
import com.adrianperezcobo.dummycommerce.users.auth.application.port.out.AccessTokenPort;
import com.adrianperezcobo.dummycommerce.users.auth.application.port.out.PasswordEncoderPort;
import com.adrianperezcobo.dummycommerce.users.auth.application.port.out.RefreshTokenPort;
import com.adrianperezcobo.dummycommerce.users.auth.application.port.out.RefreshTokenRepository;
import com.adrianperezcobo.dummycommerce.users.auth.application.result.LoginResult;
import com.adrianperezcobo.dummycommerce.users.auth.domain.RefreshToken;
import com.adrianperezcobo.dummycommerce.users.user.application.port.out.UserRepository;
import com.adrianperezcobo.dummycommerce.users.user.domain.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Service
@Transactional
public class LoginService implements LoginUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoderPort passwordEncoder;
    private final AccessTokenPort accessTokenPort;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenPort refreshTokenPort;

    public LoginService(
            UserRepository userRepository,
            PasswordEncoderPort passwordEncoder,
            AccessTokenPort accessTokenPort,
            RefreshTokenRepository refreshTokenRepository,
            RefreshTokenPort refreshTokenPort
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.accessTokenPort = accessTokenPort;
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenPort = refreshTokenPort;
    }

    @Override
    public LoginResult login(LoginUserCommand command) {

        String normalizedEmail = command.email()
                .trim()
                .toLowerCase(Locale.ROOT);

        User user = userRepository
                .findByEmail(normalizedEmail)
                .orElseThrow(
                        InvalidCredentialsException::new
                );

        if (!passwordEncoder.matches(
                command.password(),
                user.getPasswordHash()
        )) {
            throw new InvalidCredentialsException();
        }

        if (!user.isEnabled()) {
            throw new UserDisabledException();
        }

        String accessToken =
                accessTokenPort.generate(user);

        String rawRefreshToken =
                refreshTokenPort.generate();

        Instant now = Instant.now();

        RefreshToken refreshToken =
                new RefreshToken(
                        UUID.randomUUID(),
                        user.getId(),
                        refreshTokenPort.hash(
                                rawRefreshToken
                        ),
                        now.plusSeconds(
                                refreshTokenPort
                                        .getExpirationSeconds()
                        ),
                        now,
                        null
                );

        refreshTokenRepository.save(refreshToken);

        return new LoginResult(
                accessToken,
                rawRefreshToken,
                accessTokenPort.getExpirationSeconds(),
                refreshTokenPort.getExpirationSeconds()
        );
    }
}