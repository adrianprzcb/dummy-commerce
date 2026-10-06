package com.adrianperezcobo.dummycommerce.notifications.notification.application.exception;

import java.util.UUID;

public class NotificationNotFoundException
        extends RuntimeException {

    public NotificationNotFoundException(
            UUID notificationId
    ) {
        super(
                "Notification not found: "
                        + notificationId
        );
    }
}