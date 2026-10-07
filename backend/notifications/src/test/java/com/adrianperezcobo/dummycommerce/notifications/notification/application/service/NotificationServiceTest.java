package com.adrianperezcobo.dummycommerce.notifications.notification.application.service;

import com.adrianperezcobo.dummycommerce.notifications.notification.application.command.CreateNotificationCommand;
import com.adrianperezcobo.dummycommerce.notifications.notification.application.exception.NotificationForSourceEventNotFoundException;
import com.adrianperezcobo.dummycommerce.notifications.notification.application.exception.NotificationNotFoundException;
import com.adrianperezcobo.dummycommerce.notifications.notification.application.port.out.NotificationRepository;
import com.adrianperezcobo.dummycommerce.notifications.notification.application.port.out.NotificationSenderPort;
import com.adrianperezcobo.dummycommerce.notifications.notification.application.port.out.NotificationSenderResult;
import com.adrianperezcobo.dummycommerce.notifications.notification.domain.Notification;
import com.adrianperezcobo.dummycommerce.notifications.notification.domain.NotificationChannel;
import com.adrianperezcobo.dummycommerce.notifications.notification.domain.NotificationStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationSenderPort notificationSender;

    @InjectMocks
    private NotificationService service;

    @Test
    void shouldCreateAndSendNotification() {
        CreateNotificationCommand command =
                command();

        when(notificationRepository
                .findBySourceEventIdForUpdate(
                        command.sourceEventId()
                ))
                .thenReturn(Optional.empty());

        when(notificationRepository
                .save(any(Notification.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        when(notificationSender.send(
                any(),
                any(),
                any(),
                any(),
                any()
        )).thenReturn(
                NotificationSenderResult.SUCCESS
        );

        Notification result =
                service.create(command);

        assertThat(result.getStatus())
                .isEqualTo(NotificationStatus.SENT);

        assertThat(result.getSentAt())
                .isNotNull();

        verify(notificationRepository, times(2))
                .save(any(Notification.class));

        verify(notificationSender, times(1))
                .send(
                        result.getSourceEventId(),
                        result.getChannel(),
                        result.getRecipient(),
                        result.getSubject(),
                        result.getMessage()
                );
    }

    @Test
    void shouldStoreFailedNotificationWhenSenderFails() {
        CreateNotificationCommand command =
                command();

        when(notificationRepository
                .findBySourceEventIdForUpdate(
                        command.sourceEventId()
                ))
                .thenReturn(Optional.empty());

        when(notificationRepository
                .save(any(Notification.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        when(notificationSender.send(
                any(),
                any(),
                any(),
                any(),
                any()
        )).thenReturn(
                NotificationSenderResult.FAILURE
        );

        Notification result =
                service.create(command);

        assertThat(result.getStatus())
                .isEqualTo(NotificationStatus.FAILED);

        assertThat(result.getSentAt())
                .isNull();
    }

    @Test
    void shouldReturnExistingSentNotificationWithoutSendingAgain() {
        Notification existing =
                notification(
                        NotificationStatus.SENT
                );

        when(notificationRepository
                .findBySourceEventIdForUpdate(
                        existing.getSourceEventId()
                ))
                .thenReturn(Optional.of(existing));

        Notification result =
                service.create(
                        commandFor(existing)
                );

        assertThat(result)
                .isSameAs(existing);

        verifyNoInteractions(notificationSender);

        verify(notificationRepository, never())
                .save(any());
    }

    @Test
    void shouldReturnExistingFailedNotificationWithoutSendingAgain() {
        Notification existing =
                notification(
                        NotificationStatus.FAILED
                );

        when(notificationRepository
                .findBySourceEventIdForUpdate(
                        existing.getSourceEventId()
                ))
                .thenReturn(Optional.of(existing));

        Notification result =
                service.create(
                        commandFor(existing)
                );

        assertThat(result)
                .isSameAs(existing);

        verifyNoInteractions(notificationSender);
    }

    @Test
    void shouldRetryExistingPendingNotification() {
        Notification existing =
                notification(
                        NotificationStatus.PENDING
                );

        when(notificationRepository
                .findBySourceEventIdForUpdate(
                        existing.getSourceEventId()
                ))
                .thenReturn(Optional.of(existing));

        when(notificationSender.send(
                existing.getSourceEventId(),
                existing.getChannel(),
                existing.getRecipient(),
                existing.getSubject(),
                existing.getMessage()
        )).thenReturn(
                NotificationSenderResult.SUCCESS
        );

        when(notificationRepository.save(existing))
                .thenReturn(existing);

        Notification result =
                service.create(
                        commandFor(existing)
                );

        assertThat(result.getStatus())
                .isEqualTo(NotificationStatus.SENT);

        verify(notificationSender, times(1))
                .send(
                        existing.getSourceEventId(),
                        existing.getChannel(),
                        existing.getRecipient(),
                        existing.getSubject(),
                        existing.getMessage()
                );
    }

    @Test
    void shouldGetNotificationById() {
        Notification notification =
                notification(
                        NotificationStatus.SENT
                );

        when(notificationRepository.findById(
                notification.getId()
        )).thenReturn(
                Optional.of(notification)
        );

        assertThat(
                service.getById(
                        notification.getId()
                )
        ).isSameAs(notification);
    }

    @Test
    void shouldThrowWhenNotificationDoesNotExist() {
        UUID notificationId =
                UUID.randomUUID();

        when(notificationRepository.findById(
                notificationId
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.getById(notificationId)
        ).isInstanceOf(
                NotificationNotFoundException.class
        );
    }

    @Test
    void shouldGetNotificationBySourceEventId() {
        Notification notification =
                notification(
                        NotificationStatus.SENT
                );

        when(notificationRepository
                .findBySourceEventId(
                        notification.getSourceEventId()
                ))
                .thenReturn(
                        Optional.of(notification)
                );

        assertThat(
                service.getBySourceEventId(
                        notification.getSourceEventId()
                )
        ).isSameAs(notification);
    }

    @Test
    void shouldThrowWhenSourceEventHasNoNotification() {
        UUID sourceEventId =
                UUID.randomUUID();

        when(notificationRepository
                .findBySourceEventId(
                        sourceEventId
                ))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.getBySourceEventId(
                        sourceEventId
                )
        ).isInstanceOf(
                NotificationForSourceEventNotFoundException.class
        );
    }

    @Test
    void shouldGetNotificationsByUser() {
        UUID userId = UUID.randomUUID();

        Notification first =
                notification(
                        userId,
                        NotificationStatus.SENT
                );

        Notification second =
                notification(
                        userId,
                        NotificationStatus.FAILED
                );

        when(notificationRepository
                .findByUserId(userId))
                .thenReturn(
                        List.of(first, second)
                );

        assertThat(
                service.getByUserId(userId)
        ).containsExactly(
                first,
                second
        );
    }

    private CreateNotificationCommand command() {
        return new CreateNotificationCommand(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                NotificationChannel.EMAIL,
                "test@example.com",
                "Order confirmed",
                "Your order has been confirmed."
        );
    }

    private CreateNotificationCommand commandFor(
            Notification notification
    ) {
        return new CreateNotificationCommand(
                notification.getSourceEventId(),
                notification.getUserId(),
                notification.getOrderId(),
                notification.getChannel(),
                notification.getRecipient(),
                notification.getSubject(),
                notification.getMessage()
        );
    }

    private Notification notification(
            NotificationStatus status
    ) {
        return notification(
                UUID.randomUUID(),
                status
        );
    }

    private Notification notification(
            UUID userId,
            NotificationStatus status
    ) {
        Instant now = Instant.now();

        Instant sentAt =
                status == NotificationStatus.SENT
                        ? now
                        : null;

        return new Notification(
                UUID.randomUUID(),
                UUID.randomUUID(),
                userId,
                UUID.randomUUID(),
                NotificationChannel.EMAIL,
                "test@example.com",
                "Order confirmed",
                "Your order has been confirmed.",
                status,
                now,
                sentAt
        );
    }
}