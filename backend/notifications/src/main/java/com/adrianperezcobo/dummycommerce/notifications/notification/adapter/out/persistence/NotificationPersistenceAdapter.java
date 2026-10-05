package com.adrianperezcobo.dummycommerce.notifications.notification.adapter.out.persistence;

import com.adrianperezcobo.dummycommerce.notifications.notification.application.port.out.NotificationRepository;
import com.adrianperezcobo.dummycommerce.notifications.notification.domain.Notification;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class NotificationPersistenceAdapter
        implements NotificationRepository {

    private final NotificationJpaRepository repository;

    public NotificationPersistenceAdapter(
            NotificationJpaRepository repository
    ) {
        this.repository = repository;
    }

    @Override
    public Notification save(
            Notification notification
    ) {
        NotificationJpaEntity entity =
                toEntity(notification);

        return toDomain(
                repository.save(entity)
        );
    }

    @Override
    public Optional<Notification> findById(
            UUID notificationId
    ) {
        return repository
                .findById(notificationId)
                .map(this::toDomain);
    }

    @Override
    public Optional<Notification> findByIdForUpdate(
            UUID notificationId
    ) {
        return repository
                .findByIdForUpdate(
                        notificationId
                )
                .map(this::toDomain);
    }

    @Override
    public Optional<Notification> findBySourceEventId(
            UUID sourceEventId
    ) {
        return repository
                .findBySourceEventId(
                        sourceEventId
                )
                .map(this::toDomain);
    }

    @Override
    public Optional<Notification>
    findBySourceEventIdForUpdate(
            UUID sourceEventId
    ) {
        return repository
                .findBySourceEventIdForUpdate(
                        sourceEventId
                )
                .map(this::toDomain);
    }

    @Override
    public boolean existsBySourceEventId(
            UUID sourceEventId
    ) {
        return repository
                .existsBySourceEventId(
                        sourceEventId
                );
    }

    @Override
    public List<Notification> findByUserId(
            UUID userId
    ) {
        return repository
                .findByUserIdOrderByCreatedAtDesc(
                        userId
                )
                .stream()
                .map(this::toDomain)
                .toList();
    }

    private NotificationJpaEntity toEntity(
            Notification notification
    ) {
        return new NotificationJpaEntity(
                notification.getId(),
                notification.getSourceEventId(),
                notification.getUserId(),
                notification.getOrderId(),
                notification.getChannel(),
                notification.getRecipient(),
                notification.getSubject(),
                notification.getMessage(),
                notification.getStatus(),
                notification.getCreatedAt(),
                notification.getSentAt()
        );
    }

    private Notification toDomain(
            NotificationJpaEntity entity
    ) {
        return new Notification(
                entity.getId(),
                entity.getSourceEventId(),
                entity.getUserId(),
                entity.getOrderId(),
                entity.getChannel(),
                entity.getRecipient(),
                entity.getSubject(),
                entity.getMessage(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getSentAt()
        );
    }
}