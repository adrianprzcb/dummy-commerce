package com.adrianperezcobo.dummycommerce.users.auth.adapter.out.persistence;

import com.adrianperezcobo.dummycommerce.users.auth.application.port.out.RefreshTokenRepository;
import com.adrianperezcobo.dummycommerce.users.auth.domain.RefreshToken;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class RefreshTokenPersistenceAdapter
        implements RefreshTokenRepository {

    private final RefreshTokenJpaRepository repository;

    public RefreshTokenPersistenceAdapter(
            RefreshTokenJpaRepository repository
    ) {
        this.repository = repository;
    }

    @Override
    public RefreshToken save(RefreshToken refreshToken) {

        RefreshTokenJpaEntity entity =
                new RefreshTokenJpaEntity(
                        refreshToken.getId(),
                        refreshToken.getUserId(),
                        refreshToken.getTokenHash(),
                        refreshToken.getExpiresAt(),
                        refreshToken.getCreatedAt(),
                        refreshToken.getRevokedAt()
                );

        return toDomain(repository.save(entity));
    }

    @Override
    public Optional<RefreshToken> findByTokenHashForUpdate(
            String tokenHash
    ) {
        return repository
                .findByTokenHashForUpdate(tokenHash)
                .map(this::toDomain);
    }

    private RefreshToken toDomain(
            RefreshTokenJpaEntity entity
    ) {
        return new RefreshToken(
                entity.getId(),
                entity.getUserId(),
                entity.getTokenHash(),
                entity.getExpiresAt(),
                entity.getCreatedAt(),
                entity.getRevokedAt()
        );
    }
}