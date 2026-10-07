package com.adrianperezcobo.dummycommerce.notifications.notification.adapter.in.web;

import com.adrianperezcobo.dummycommerce.notifications.notification.adapter.in.web.request.CreateNotificationRequest;
import com.adrianperezcobo.dummycommerce.notifications.notification.adapter.in.web.response.NotificationResponse;
import com.adrianperezcobo.dummycommerce.notifications.notification.application.command.CreateNotificationCommand;
import com.adrianperezcobo.dummycommerce.notifications.notification.domain.Notification;
import org.springframework.stereotype.Component;

@Component
public class NotificationWebMapper {

    public CreateNotificationCommand toCommand(
            CreateNotificationRequest request
    ) {
        return new CreateNotificationCommand(
                request.sourceEventId(),
                request.userId(),
                request.orderId(),
                request.channel(),
                request.recipient(),
                request.subject(),
                request.message()
        );
    }

    public NotificationResponse toResponse(
            Notification notification
    ) {
        return new NotificationResponse(
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
}