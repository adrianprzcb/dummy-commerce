package com.adrianperezcobo.dummycommerce.notifications.user.application.port.out;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface UserContactPort {
    void upsert(UUID userId, String email, Instant occurredAt);
    Optional<String> findEmail(UUID userId);
}
