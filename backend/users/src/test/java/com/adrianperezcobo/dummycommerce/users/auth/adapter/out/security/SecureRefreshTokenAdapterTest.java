package com.adrianperezcobo.dummycommerce.users.auth.adapter.out.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SecureRefreshTokenAdapterTest {

    private final SecureRefreshTokenAdapter adapter =
            new SecureRefreshTokenAdapter(
                    new RefreshTokenProperties(
                            2_592_000
                    )
            );

    @Test
    void shouldGenerateDifferentTokens() {
        String first = adapter.generate();
        String second = adapter.generate();

        assertThat(first).isNotBlank();
        assertThat(second).isNotBlank();

        assertThat(first)
                .isNotEqualTo(second);
    }

    @Test
    void shouldGenerateDeterministicHash() {
        String hash1 =
                adapter.hash("REFRESH_TOKEN");

        String hash2 =
                adapter.hash("REFRESH_TOKEN");

        assertThat(hash1)
                .isEqualTo(hash2);

        assertThat(hash1)
                .hasSize(64);
    }

    @Test
    void differentTokensShouldProduceDifferentHashes() {
        assertThat(
                adapter.hash("TOKEN_A")
        ).isNotEqualTo(
                adapter.hash("TOKEN_B")
        );
    }

    @Test
    void shouldReturnConfiguredExpiration() {
        assertThat(
                adapter.getExpirationSeconds()
        ).isEqualTo(2_592_000);
    }
}