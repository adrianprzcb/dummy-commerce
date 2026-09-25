package com.adrianperezcobo.dummycommerce.users.auth.adapter.out.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenJpaRepository
        extends JpaRepository<RefreshTokenJpaEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT r
            FROM RefreshTokenJpaEntity r
            WHERE r.tokenHash = :tokenHash
            """)
    Optional<RefreshTokenJpaEntity> findByTokenHashForUpdate(
            @Param("tokenHash") String tokenHash
    );
}