package com.adrianperezcobo.dummycommerce.notifications.notification.application.service;

import com.adrianperezcobo.dummycommerce.notifications.notification.application.command.CreateNotificationCommand;
import com.adrianperezcobo.dummycommerce.notifications.notification.application.exception.UserContactNotAvailableException;
import com.adrianperezcobo.dummycommerce.notifications.notification.application.integration.orders.*;
import com.adrianperezcobo.dummycommerce.notifications.notification.domain.NotificationChannel;
import com.adrianperezcobo.dummycommerce.notifications.shared.inbox.InboxPort;
import com.adrianperezcobo.dummycommerce.notifications.user.application.integration.users.*;
import com.adrianperezcobo.dummycommerce.notifications.user.application.port.out.UserContactPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
@Transactional
public class NotificationEventHandler {
    private final InboxPort inbox;
    private final UserContactPort contacts;
    private final NotificationService notifications;

    public NotificationEventHandler(InboxPort inbox, UserContactPort contacts, NotificationService notifications) {
        this.inbox = inbox;
        this.contacts = contacts;
        this.notifications = notifications;
    }

    public void handle(UserRegisteredEventV1 event) {
        if (!inbox.claim(event.messageId(), UsersTopics.USER_REGISTERED_V1)) return;
        contacts.upsert(event.userId(), event.email(), event.occurredAt());
    }

    public void handle(OrderConfirmedEventV1 event) {
        if (!inbox.claim(event.messageId(), OrdersTopics.ORDER_CONFIRMED_V1)) return;
        notify(event.messageId(), event.userId(), event.orderId(), "Order confirmed", "confirmed");
    }

    public void handle(OrderCancelledEventV1 event) {
        if (!inbox.claim(event.messageId(), OrdersTopics.ORDER_CANCELLED_V1)) return;
        notify(event.messageId(), event.userId(), event.orderId(), "Order cancelled", "cancelled");
    }

    private void notify(UUID sourceEventId, UUID userId, UUID orderId, String subject, String outcome) {
        String recipient = contacts.findEmail(userId).orElseThrow(() -> new UserContactNotAvailableException(userId));
        notifications.create(new CreateNotificationCommand(sourceEventId, userId, orderId, NotificationChannel.EMAIL,
                recipient, subject, "Your order " + orderId + " has been " + outcome + "."));
    }
}
