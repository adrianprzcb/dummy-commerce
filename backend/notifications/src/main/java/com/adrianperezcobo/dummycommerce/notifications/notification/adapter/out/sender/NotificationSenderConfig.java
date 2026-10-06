package com.adrianperezcobo.dummycommerce.notifications.notification.adapter.out.sender;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(
        NotificationSimulatorProperties.class
)
public class NotificationSenderConfig {
}