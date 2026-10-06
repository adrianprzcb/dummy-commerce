package com.adrianperezcobo.dummycommerce.notifications.notification.application.port.in;

import com.adrianperezcobo.dummycommerce.notifications.notification.application.command.CreateNotificationCommand;
import com.adrianperezcobo.dummycommerce.notifications.notification.domain.Notification;

public interface CreateNotificationUseCase {

    Notification create(
            CreateNotificationCommand command
    );
}