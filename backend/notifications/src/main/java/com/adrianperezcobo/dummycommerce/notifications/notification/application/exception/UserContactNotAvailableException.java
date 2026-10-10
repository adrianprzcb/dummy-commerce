package com.adrianperezcobo.dummycommerce.notifications.notification.application.exception;

import java.util.UUID;

public class UserContactNotAvailableException extends RuntimeException {
    public UserContactNotAvailableException(UUID userId) { super("User contact not available: " + userId); }
}
