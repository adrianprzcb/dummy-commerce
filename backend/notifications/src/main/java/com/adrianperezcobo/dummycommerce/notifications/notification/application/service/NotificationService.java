package com.adrianperezcobo.dummycommerce.notifications.notification.application.service;

import com.adrianperezcobo.dummycommerce.notifications.notification.application.command.CreateNotificationCommand;
import com.adrianperezcobo.dummycommerce.notifications.notification.application.exception.NotificationForSourceEventNotFoundException;
import com.adrianperezcobo.dummycommerce.notifications.notification.application.exception.NotificationNotFoundException;
import com.adrianperezcobo.dummycommerce.notifications.notification.application.port.in.CreateNotificationUseCase;
import com.adrianperezcobo.dummycommerce.notifications.notification.application.port.in.GetNotificationBySourceEventUseCase;
import com.adrianperezcobo.dummycommerce.notifications.notification.application.port.in.GetNotificationUseCase;
import com.adrianperezcobo.dummycommerce.notifications.notification.application.port.in.GetUserNotificationsUseCase;
import com.adrianperezcobo.dummycommerce.notifications.notification.application.port.out.NotificationRepository;
import com.adrianperezcobo.dummycommerce.notifications.notification.application.port.out.NotificationSenderPort;
import com.adrianperezcobo.dummycommerce.notifications.notification.application.port.out.NotificationSenderResult;
import com.adrianperezcobo.dummycommerce.notifications.notification.domain.Notification;
import com.adrianperezcobo.dummycommerce.notifications.notification.domain.NotificationStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class NotificationService implements
        CreateNotificationUseCase,
        GetNotificationUseCase,
        GetNotificationBySourceEventUseCase,
        GetUserNotificationsUseCase {

    private final NotificationRepository notificationRepository;
    private final NotificationSenderPort notificationSender;

    public NotificationService(
            NotificationRepository notificationRepository,
            NotificationSenderPort notificationSender
    ) {
        this.notificationRepository =
                notificationRepository;

        this.notificationSender =
                notificationSender;
    }

    @Override
    @Transactional
    public Notification create(
            CreateNotificationCommand command
    ) {
        Notification existing =
                notificationRepository
                        .findBySourceEventIdForUpdate(
                                command.sourceEventId()
                        )
                        .orElse(null);

        if (existing != null) {

            /*
             * SENT o FAILED:
             * el mismo evento no genera otra notificación.
             */
            if (existing.getStatus()
                    != NotificationStatus.PENDING) {

                return existing;
            }

            /*
             * Si quedó PENDING por una caída anterior,
             * reintentamos procesarla.
             */
            return sendPending(existing);
        }

        Instant now = Instant.now();

        Notification notification =
                new Notification(
                        UUID.randomUUID(),
                        command.sourceEventId(),
                        command.userId(),
                        command.orderId(),
                        command.channel(),
                        command.recipient(),
                        command.subject(),
                        command.message(),
                        NotificationStatus.PENDING,
                        now,
                        null
                );

        notification =
                notificationRepository.save(
                        notification
                );

        return sendPending(notification);
    }

    @Override
    @Transactional(readOnly = true)
    public Notification getById(
            UUID notificationId
    ) {
        return notificationRepository
                .findById(notificationId)
                .orElseThrow(
                        () ->
                                new NotificationNotFoundException(
                                        notificationId
                                )
                );
    }

    @Override
    @Transactional(readOnly = true)
    public Notification getBySourceEventId(
            UUID sourceEventId
    ) {
        return notificationRepository
                .findBySourceEventId(
                        sourceEventId
                )
                .orElseThrow(
                        () ->
                                new NotificationForSourceEventNotFoundException(
                                        sourceEventId
                                )
                );
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> getByUserId(
            UUID userId
    ) {
        return notificationRepository
                .findByUserId(userId);
    }

    private Notification sendPending(
            Notification notification
    ) {
        NotificationSenderResult result =
                notificationSender.send(
                        notification.getSourceEventId(),
                        notification.getChannel(),
                        notification.getRecipient(),
                        notification.getSubject(),
                        notification.getMessage()
                );

        if (result ==
                NotificationSenderResult.SUCCESS) {

            notification.markSent(
                    Instant.now()
            );

        } else {
            notification.markFailed();
        }

        return notificationRepository.save(
                notification
        );
    }
}