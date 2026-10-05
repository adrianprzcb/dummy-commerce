package com.adrianperezcobo.dummycommerce.notifications.notification.adapter.out.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationJpaRepository
        extends JpaRepository<
        NotificationJpaEntity,
        UUID
        > {

    Optional<NotificationJpaEntity>
    findBySourceEventId(
            UUID sourceEventId
    );

    boolean existsBySourceEventId(
            UUID sourceEventId
    );

    List<NotificationJpaEntity>
    findByUserIdOrderByCreatedAtDesc(
            UUID userId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT n
            FROM NotificationJpaEntity n
            WHERE n.id = :notificationId
            """)
    Optional<NotificationJpaEntity>
    findByIdForUpdate(
            @Param("notificationId")
            UUID notificationId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT n
            FROM NotificationJpaEntity n
            WHERE n.sourceEventId = :sourceEventId
            """)
    Optional<NotificationJpaEntity>
    findBySourceEventIdForUpdate(
            @Param("sourceEventId")
            UUID sourceEventId
    );
}