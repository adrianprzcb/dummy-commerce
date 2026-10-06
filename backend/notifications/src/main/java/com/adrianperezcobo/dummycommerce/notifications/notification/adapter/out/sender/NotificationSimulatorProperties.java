package com.adrianperezcobo.dummycommerce.notifications.notification.adapter.out.sender;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(
        prefix = "notification.simulator"
)
public record NotificationSimulatorProperties(
        String mode
) {
}