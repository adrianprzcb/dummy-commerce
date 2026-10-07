package com.adrianperezcobo.dummycommerce.notifications.notification.adapter.in.web.request;

import com.adrianperezcobo.dummycommerce.notifications.notification.domain.NotificationChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateNotificationRequest(

        @NotNull
        UUID sourceEventId,

        @NotNull
        UUID userId,

        UUID orderId,

        @NotNull
        NotificationChannel channel,

        @NotBlank
        @Size(max = 320)
        String recipient,

        @NotBlank
        @Size(max = 200)
        String subject,

        @NotBlank
        String message
) {
}