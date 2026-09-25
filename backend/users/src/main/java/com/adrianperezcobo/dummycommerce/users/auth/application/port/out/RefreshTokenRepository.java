package com.adrianperezcobo.dummycommerce.users.auth.application.port.out;

import com.adrianperezcobo.dummycommerce.users.auth.domain.RefreshToken;

import java.util.Optional;

public interface RefreshTokenRepository {

    RefreshToken save(RefreshToken refreshToken);

    Optional<RefreshToken> findByTokenHashForUpdate(
            String tokenHash
    );
}