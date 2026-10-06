package com.adrianperezcobo.dummycommerce.notifications.notification.adapter.out.sender;

import com.adrianperezcobo.dummycommerce.notifications.notification.application.port.out.NotificationSenderPort;
import com.adrianperezcobo.dummycommerce.notifications.notification.application.port.out.NotificationSenderResult;
import com.adrianperezcobo.dummycommerce.notifications.notification.domain.NotificationChannel;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.UUID;

@Component
public class SimulatedNotificationSenderAdapter
        implements NotificationSenderPort {

    private final NotificationSimulatorProperties properties;

    public SimulatedNotificationSenderAdapter(
            NotificationSimulatorProperties properties
    ) {
        this.properties = properties;
    }

    @Override
    public NotificationSenderResult send(
            UUID sourceEventId,
            NotificationChannel channel,
            String recipient,
            String subject,
            String message
    ) {
        String mode = properties.mode();

        if (mode == null) {
            throw new IllegalStateException(
                    "Notification simulator mode is required"
            );
        }

        return switch (
                mode.trim()
                        .toLowerCase(Locale.ROOT)
                ) {
            case "success" ->
                    NotificationSenderResult.SUCCESS;

            case "failure" ->
                    NotificationSenderResult.FAILURE;

            default ->
                    throw new IllegalStateException(
                            "Unsupported notification simulator mode: "
                                    + mode
                    );
        };
    }
}