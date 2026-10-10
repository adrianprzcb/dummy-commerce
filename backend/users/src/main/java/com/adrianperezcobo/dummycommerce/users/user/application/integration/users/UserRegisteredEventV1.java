package com.adrianperezcobo.dummycommerce.users.user.application.integration.users;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record UserRegisteredEventV1(UUID messageId, UUID userId, String email, Instant occurredAt) {
    public UserRegisteredEventV1 {
        Objects.requireNonNull(messageId, "messageId");
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(occurredAt, "occurredAt");
        if (email == null || email.isBlank() || email.length() > 320) {
            throw new IllegalArgumentException("Invalid user contact email");
        }
    }
}
