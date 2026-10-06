package com.adrianperezcobo.dummycommerce.notifications.notification.application.exception;

import java.util.UUID;

public class NotificationForSourceEventNotFoundException
        extends RuntimeException {

    public NotificationForSourceEventNotFoundException(
            UUID sourceEventId
    ) {
        super(
                "Notification not found for source event: "
                        + sourceEventId
        );
    }
}