package com.adrianperezcobo.dummycommerce.users.auth.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RefreshTokenTest {

    @Test
    void shouldBeActiveInitially() {
        Instant now = Instant.now();

        RefreshToken token = createToken(
                now.plusSeconds(3600),
                null
        );

        assertThat(token.isRevoked()).isFalse();
        assertThat(token.isExpired(now)).isFalse();
    }

    @Test
    void shouldDetectExpiredToken() {
        Instant now = Instant.now();

        RefreshToken token = createToken(
                now.minusSeconds(1),
                null
        );

        assertThat(token.isExpired(now)).isTrue();
    }

    @Test
    void shouldRevokeToken() {
        RefreshToken token = createToken(
                Instant.now().plusSeconds(3600),
                null
        );

        Instant revokedAt = Instant.now();

        token.revoke(revokedAt);

        assertThat(token.isRevoked()).isTrue();

        assertThat(token.getRevokedAt())
                .isEqualTo(revokedAt);
    }

    @Test
    void revokeShouldBeIdempotent() {
        RefreshToken token = createToken(
                Instant.now().plusSeconds(3600),
                null
        );

        Instant first = Instant.now();
        Instant second = first.plusSeconds(10);

        token.revoke(first);
        token.revoke(second);

        assertThat(token.getRevokedAt())
                .isEqualTo(first);
    }

    @Test
    void shouldRejectBlankHash() {
        assertThatThrownBy(() ->
                new RefreshToken(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        " ",
                        Instant.now().plusSeconds(3600),
                        Instant.now(),
                        null
                )
        ).isInstanceOf(
                IllegalArgumentException.class
        );
    }

    private RefreshToken createToken(
            Instant expiresAt,
            Instant revokedAt
    ) {
        return new RefreshToken(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "a".repeat(64),
                expiresAt,
                Instant.now(),
                revokedAt
        );
    }
}