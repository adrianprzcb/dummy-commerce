package com.adrianperezcobo.dummycommerce.users.auth.adapter.out.security;

import com.adrianperezcobo.dummycommerce.users.auth.application.exception.InvalidAccessTokenException;
import com.adrianperezcobo.dummycommerce.users.auth.application.result.AuthenticatedUser;
import com.adrianperezcobo.dummycommerce.users.user.domain.Role;
import com.adrianperezcobo.dummycommerce.users.user.domain.User;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtAccessTokenAdapterTest {

    private static final String SECRET =
            "ZHVtbXktY29tbWVyY2UtZGV2LWp3dC1zZWNyZXQtMzJiIQ==";

    @Test
    void shouldGenerateAndParseAccessToken() {
        JwtProperties properties =
                new JwtProperties(
                        SECRET,
                        900
                );

        JwtAccessTokenAdapter adapter =
                new JwtAccessTokenAdapter(properties);

        UUID userId = UUID.randomUUID();

        User user = new User(
                userId,
                "adrian@example.com",
                "HASH",
                Role.ADMIN,
                true,
                Instant.now()
        );

        String token = adapter.generate(user);

        assertThat(token).isNotBlank();

        AuthenticatedUser authenticatedUser =
                adapter.parse(token);

        assertThat(authenticatedUser.userId())
                .isEqualTo(userId);

        assertThat(authenticatedUser.email())
                .isEqualTo("adrian@example.com");

        assertThat(authenticatedUser.role())
                .isEqualTo(Role.ADMIN);
    }

    @Test
    void shouldRejectInvalidToken() {
        JwtAccessTokenAdapter adapter =
                new JwtAccessTokenAdapter(
                        new JwtProperties(
                                SECRET,
                                900
                        )
                );

        assertThatThrownBy(() ->
                adapter.parse("not-a-jwt")
        ).isInstanceOf(
                InvalidAccessTokenException.class
        );
    }

    @Test
    void shouldRejectTokenSignedWithDifferentSecret() {
        JwtAccessTokenAdapter first =
                new JwtAccessTokenAdapter(
                        new JwtProperties(
                                SECRET,
                                900
                        )
                );

        JwtAccessTokenAdapter second =
                new JwtAccessTokenAdapter(
                        new JwtProperties(
                                "YW5vdGhlci1kdW1teS1jb21tZXJjZS1qd3Qtc2VjcmV0LTMyYg==",
                                900
                        )
                );

        User user = new User(
                UUID.randomUUID(),
                "test@example.com",
                "HASH",
                Role.USER,
                true,
                Instant.now()
        );

        String token = first.generate(user);

        assertThatThrownBy(() ->
                second.parse(token)
        ).isInstanceOf(
                InvalidAccessTokenException.class
        );
    }
}