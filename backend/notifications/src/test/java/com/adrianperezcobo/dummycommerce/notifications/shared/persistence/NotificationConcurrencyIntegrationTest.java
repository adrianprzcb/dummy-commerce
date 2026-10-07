package com.adrianperezcobo.dummycommerce.notifications.shared.persistence;

import com.adrianperezcobo.dummycommerce.notifications.notification.application.command.CreateNotificationCommand;
import com.adrianperezcobo.dummycommerce.notifications.notification.application.port.in.CreateNotificationUseCase;
import com.adrianperezcobo.dummycommerce.notifications.notification.application.port.out.NotificationRepository;
import com.adrianperezcobo.dummycommerce.notifications.notification.application.port.out.NotificationSenderPort;
import com.adrianperezcobo.dummycommerce.notifications.notification.application.port.out.NotificationSenderResult;
import com.adrianperezcobo.dummycommerce.notifications.notification.domain.Notification;
import com.adrianperezcobo.dummycommerce.notifications.notification.domain.NotificationChannel;
import com.adrianperezcobo.dummycommerce.notifications.notification.domain.NotificationStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@SpringBootTest(
        webEnvironment =
                SpringBootTest.WebEnvironment.NONE
)
@Testcontainers
class NotificationConcurrencyIntegrationTest {

    private static final int REQUESTS = 10;

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer(
                    "postgres:17-alpine"
            );

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private CreateNotificationUseCase createNotificationUseCase;

    @MockitoBean
    private NotificationSenderPort notificationSender;

    @Test
    @Timeout(30)
    void shouldSendExistingPendingNotificationOnlyOnceUnderConcurrency()
            throws Exception {

        Notification notification =
                pendingNotification();

        notificationRepository.save(
                notification
        );

        when(notificationSender.send(
                notification.getSourceEventId(),
                notification.getChannel(),
                notification.getRecipient(),
                notification.getSubject(),
                notification.getMessage()
        )).thenReturn(
                NotificationSenderResult.SUCCESS
        );

        CreateNotificationCommand command =
                new CreateNotificationCommand(
                        notification.getSourceEventId(),
                        notification.getUserId(),
                        notification.getOrderId(),
                        notification.getChannel(),
                        notification.getRecipient(),
                        notification.getSubject(),
                        notification.getMessage()
                );

        ExecutorService executor =
                Executors.newFixedThreadPool(
                        REQUESTS
                );

        CountDownLatch ready =
                new CountDownLatch(REQUESTS);

        CountDownLatch start =
                new CountDownLatch(1);

        try {
            List<Future<NotificationStatus>>
                    futures = new ArrayList<>();

            for (int i = 0; i < REQUESTS; i++) {
                futures.add(
                        executor.submit(() -> {
                            ready.countDown();

                            start.await();

                            return createNotificationUseCase
                                    .create(command)
                                    .getStatus();
                        })
                );
            }

            assertThat(
                    ready.await(
                            10,
                            TimeUnit.SECONDS
                    )
            ).isTrue();

            start.countDown();

            for (Future<NotificationStatus> future
                    : futures) {

                assertThat(
                        future.get(
                                15,
                                TimeUnit.SECONDS
                        )
                ).isEqualTo(
                        NotificationStatus.SENT
                );
            }

            Notification finalNotification =
                    notificationRepository
                            .findById(
                                    notification.getId()
                            )
                            .orElseThrow();

            assertThat(
                    finalNotification.getStatus()
            ).isEqualTo(
                    NotificationStatus.SENT
            );

            verify(
                    notificationSender,
                    times(1)
            ).send(
                    notification.getSourceEventId(),
                    notification.getChannel(),
                    notification.getRecipient(),
                    notification.getSubject(),
                    notification.getMessage()
            );

        } finally {
            executor.shutdownNow();
        }
    }

    private Notification pendingNotification() {
        return new Notification(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                NotificationChannel.EMAIL,
                "test@example.com",
                "Order confirmed",
                "Your order has been confirmed.",
                NotificationStatus.PENDING,
                Instant.now(),
                null
        );
    }
}