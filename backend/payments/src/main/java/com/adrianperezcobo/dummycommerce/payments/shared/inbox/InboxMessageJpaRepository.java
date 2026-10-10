package com.adrianperezcobo.dummycommerce.payments.shared.inbox;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.UUID;

public interface InboxMessageJpaRepository extends JpaRepository<InboxMessageJpaEntity, UUID> {
    @Modifying
    @Query(value = """
            INSERT INTO inbox_messages(message_id, topic, processed_at)
            VALUES (:id, :topic, :now)
            ON CONFLICT (message_id) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("id") UUID id, @Param("topic") String topic, @Param("now") Instant now);
    @Modifying
    @Query(value = """
            DELETE FROM inbox_messages WHERE message_id IN (
                SELECT message_id FROM inbox_messages WHERE processed_at < :cutoff
                ORDER BY processed_at LIMIT :batchSize FOR UPDATE SKIP LOCKED
            )
            """, nativeQuery = true)
    int deleteProcessedBefore(@Param("cutoff") Instant cutoff, @Param("batchSize") int batchSize);
}

