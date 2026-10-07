package com.adrianperezcobo.dummycommerce.notifications.notification.adapter.out.persistence;

import com.adrianperezcobo.dummycommerce.notifications.notification.application.port.out.NotificationRepository;
import com.adrianperezcobo.dummycommerce.notifications.notification.domain.Notification;
import com.adrianperezcobo.dummycommerce.notifications.notification.domain.NotificationChannel;
import com.adrianperezcobo.dummycommerce.notifications.notification.domain.NotificationStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.NONE;

@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@Import(NotificationPersistenceAdapter.class)
@Testcontainers
class NotificationPersistenceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer(
                    "postgres:17-alpine"
            );

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldPersistAndLoadNotification() {
        Notification notification =
                notification(
                        UUID.randomUUID(),
                        Instant.now()
                );

        notificationRepository.save(notification);

        flushAndClear();

        Notification loaded =
                notificationRepository
                        .findById(
                                notification.getId()
                        )
                        .orElseThrow();

        assertThat(loaded.getSourceEventId())
                .isEqualTo(
                        notification.getSourceEventId()
                );

        assertThat(loaded.getChannel())
                .isEqualTo(
                        NotificationChannel.EMAIL
                );

        assertThat(loaded.getStatus())
                .isEqualTo(
                        NotificationStatus.PENDING
                );
    }

    @Test
    void shouldFindBySourceEventId() {
        Notification notification =
                notification(
                        UUID.randomUUID(),
                        Instant.now()
                );

        notificationRepository.save(notification);

        flushAndClear();

        Notification loaded =
                notificationRepository
                        .findBySourceEventId(
                                notification.getSourceEventId()
                        )
                        .orElseThrow();

        assertThat(loaded.getId())
                .isEqualTo(notification.getId());

        assertThat(
                notificationRepository
                        .existsBySourceEventId(
                                notification.getSourceEventId()
                        )
        ).isTrue();
    }

    @Test
    void shouldFindUserNotificationsNewestFirst() {
        UUID userId = UUID.randomUUID();

        Notification older =
                notification(
                        userId,
                        Instant.parse(
                                "2026-01-01T10:00:00Z"
                        )
                );

        Notification newer =
                notification(
                        userId,
                        Instant.parse(
                                "2026-01-02T10:00:00Z"
                        )
                );

        notificationRepository.save(older);
        notificationRepository.save(newer);

        flushAndClear();

        List<Notification> notifications =
                notificationRepository
                        .findByUserId(userId);

        assertThat(notifications)
                .extracting(Notification::getId)
                .containsExactly(
                        newer.getId(),
                        older.getId()
                );
    }

    @Test
    void shouldLoadBySourceEventIdForUpdate() {
        Notification notification =
                notification(
                        UUID.randomUUID(),
                        Instant.now()
                );

        notificationRepository.save(notification);

        flushAndClear();

        Notification loaded =
                notificationRepository
                        .findBySourceEventIdForUpdate(
                                notification
                                        .getSourceEventId()
                        )
                        .orElseThrow();

        assertThat(loaded.getId())
                .isEqualTo(notification.getId());
    }

    private Notification notification(
            UUID userId,
            Instant createdAt
    ) {
        return new Notification(
                UUID.randomUUID(),
                UUID.randomUUID(),
                userId,
                UUID.randomUUID(),
                NotificationChannel.EMAIL,
                "test@example.com",
                "Order confirmed",
                "Your order has been confirmed.",
                NotificationStatus.PENDING,
                createdAt,
                null
        );
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}