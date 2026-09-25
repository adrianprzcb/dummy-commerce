package com.adrianperezcobo.dummycommerce.users.auth.application.service;

import com.adrianperezcobo.dummycommerce.users.auth.application.command.LoginUserCommand;
import com.adrianperezcobo.dummycommerce.users.auth.application.exception.InvalidCredentialsException;
import com.adrianperezcobo.dummycommerce.users.auth.application.exception.UserDisabledException;
import com.adrianperezcobo.dummycommerce.users.auth.application.port.in.LoginUseCase;
import com.adrianperezcobo.dummycommerce.users.auth.application.port.out.AccessTokenPort;
import com.adrianperezcobo.dummycommerce.users.auth.application.port.out.PasswordEncoderPort;
import com.adrianperezcobo.dummycommerce.users.auth.application.result.LoginResult;
import com.adrianperezcobo.dummycommerce.users.user.application.port.out.UserRepository;
import com.adrianperezcobo.dummycommerce.users.user.domain.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class LoginService implements LoginUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoderPort passwordEncoder;
    private final AccessTokenPort accessTokenPort;

    public LoginService(
            UserRepository userRepository,
            PasswordEncoderPort passwordEncoder,
            AccessTokenPort accessTokenPort
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.accessTokenPort = accessTokenPort;
    }

    @Override
    public LoginResult login(LoginUserCommand command) {

        String normalizedEmail = command.email()
                .trim()
                .toLowerCase(Locale.ROOT);

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(InvalidCredentialsException::new);

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

        return new LoginResult(
                accessToken,
                accessTokenPort.getExpirationSeconds()
        );
    }
}