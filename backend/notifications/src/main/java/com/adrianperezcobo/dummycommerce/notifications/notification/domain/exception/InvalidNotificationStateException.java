package com.adrianperezcobo.dummycommerce.notifications.notification.domain.exception;

public class InvalidNotificationStateException
        extends RuntimeException {

    public InvalidNotificationStateException(
            String message
    ) {
        super(message);
    }
}