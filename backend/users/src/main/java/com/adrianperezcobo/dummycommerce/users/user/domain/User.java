package com.adrianperezcobo.dummycommerce.users.user.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class User {

    private final UUID id;
    private String email;
    private String passwordHash;
    private Role role;
    private boolean enabled;
    private final Instant createdAt;

    public User(
            UUID id,
            String email,
            String passwordHash,
            Role role,
            boolean enabled,
            Instant createdAt
    ) {
        this.id = Objects.requireNonNull(id);
        this.role = Objects.requireNonNull(role);
        this.createdAt = Objects.requireNonNull(createdAt);

        changeEmail(email);
        changePasswordHash(passwordHash);

        this.enabled = enabled;
    }

    public void changeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException(
                    "User email cannot be empty"
            );
        }

        this.email = normalizeEmail(email);
    }

    public void changePasswordHash(String passwordHash) {
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException(
                    "Password hash cannot be empty"
            );
        }

        this.passwordHash = passwordHash;
    }

    public void changeRole(Role role) {
        this.role = Objects.requireNonNull(role);
    }

    public void enable() {
        this.enabled = true;
    }

    public void disable() {
        this.enabled = false;
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}