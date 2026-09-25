package com.adrianperezcobo.dummycommerce.users.user.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTest {

    @Test
    void shouldNormalizeEmail() {
        User user = createUser(
                "  Adrian@Example.COM  ",
                true
        );

        assertThat(user.getEmail())
                .isEqualTo("adrian@example.com");
    }

    @Test
    void shouldChangeEmail() {
        User user = createUser(
                "old@example.com",
                true
        );

        user.changeEmail(" NEW@Example.com ");

        assertThat(user.getEmail())
                .isEqualTo("new@example.com");
    }

    @Test
    void shouldRejectBlankEmail() {
        assertThatThrownBy(() ->
                createUser("   ", true)
        ).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectBlankPasswordHash() {
        assertThatThrownBy(() ->
                new User(
                        UUID.randomUUID(),
                        "test@example.com",
                        " ",
                        Role.USER,
                        true,
                        Instant.now()
                )
        ).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldDisableAndEnableUser() {
        User user = createUser(
                "test@example.com",
                true
        );

        user.disable();

        assertThat(user.isEnabled()).isFalse();

        user.enable();

        assertThat(user.isEnabled()).isTrue();
    }

    @Test
    void shouldChangeRole() {
        User user = createUser(
                "test@example.com",
                true
        );

        user.changeRole(Role.ADMIN);

        assertThat(user.getRole())
                .isEqualTo(Role.ADMIN);
    }

    private User createUser(
            String email,
            boolean enabled
    ) {
        return new User(
                UUID.randomUUID(),
                email,
                "HASH",
                Role.USER,
                enabled,
                Instant.now()
        );
    }
}