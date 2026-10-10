package com.adrianperezcobo.dummycommerce.orders.shared.outbox;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.UUID;

public interface OutboxMessageJpaRepository
        extends JpaRepository<OutboxMessageJpaEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<OutboxMessageJpaEntity>
    findTop100ByStatusOrderByCreatedAtAscIdAsc(
            OutboxStatus status
    );
    @Modifying
    @Query(value = """
            DELETE FROM outbox_messages WHERE id IN (
                SELECT id FROM outbox_messages
                WHERE status = 'PUBLISHED' AND published_at < :cutoff
                ORDER BY published_at LIMIT :batchSize FOR UPDATE SKIP LOCKED
            )
            """, nativeQuery = true)
    int deletePublishedBefore(@Param("cutoff") Instant cutoff,
            @Param("batchSize") int batchSize);
}
