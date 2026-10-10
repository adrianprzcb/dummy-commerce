package com.adrianperezcobo.dummycommerce.notifications.user.adapter.out.persistence;

import com.adrianperezcobo.dummycommerce.notifications.user.application.port.out.UserContactPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.*;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
public class UserContactAdapter implements UserContactPort {
    private final UserContactJpaRepository repository;
    public UserContactAdapter(UserContactJpaRepository repository) { this.repository = repository; }

    @Transactional(propagation = Propagation.MANDATORY)
    public void upsert(UUID userId, String email, Instant occurredAt) { repository.upsert(userId, email, occurredAt); }

    public Optional<String> findEmail(UUID userId) { return repository.findById(userId).map(UserContactJpaEntity::getEmail); }
}
