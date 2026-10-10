package com.adrianperezcobo.dummycommerce.notifications.user.adapter.out.persistence;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.UUID;

public interface UserContactJpaRepository extends JpaRepository<UserContactJpaEntity, UUID> {
    @Modifying
    @Query(value = """
            INSERT INTO user_contacts(user_id, email, updated_at)
            VALUES (:id, :email, :occurredAt)
            ON CONFLICT (user_id) DO UPDATE
            SET email = excluded.email, updated_at = excluded.updated_at
            WHERE user_contacts.updated_at < excluded.updated_at
            """, nativeQuery = true)
    int upsert(@Param("id") UUID id, @Param("email") String email, @Param("occurredAt") Instant occurredAt);
}
