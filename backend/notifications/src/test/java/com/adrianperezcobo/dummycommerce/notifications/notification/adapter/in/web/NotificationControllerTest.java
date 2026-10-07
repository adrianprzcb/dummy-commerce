package com.adrianperezcobo.dummycommerce.notifications.notification.adapter.in.web;

import com.adrianperezcobo.dummycommerce.notifications.notification.application.command.CreateNotificationCommand;
import com.adrianperezcobo.dummycommerce.notifications.notification.application.exception.NotificationNotFoundException;
import com.adrianperezcobo.dummycommerce.notifications.notification.application.port.in.CreateNotificationUseCase;
import com.adrianperezcobo.dummycommerce.notifications.notification.application.port.in.GetNotificationBySourceEventUseCase;
import com.adrianperezcobo.dummycommerce.notifications.notification.application.port.in.GetNotificationUseCase;
import com.adrianperezcobo.dummycommerce.notifications.notification.application.port.in.GetUserNotificationsUseCase;
import com.adrianperezcobo.dummycommerce.notifications.notification.domain.Notification;
import com.adrianperezcobo.dummycommerce.notifications.notification.domain.NotificationChannel;
import com.adrianperezcobo.dummycommerce.notifications.notification.domain.NotificationStatus;
import com.adrianperezcobo.dummycommerce.notifications.shared.adapter.in.web.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NotificationController.class)
@Import({
        NotificationWebMapper.class,
        GlobalExceptionHandler.class
})
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateNotificationUseCase createNotificationUseCase;

    @MockitoBean
    private GetNotificationUseCase getNotificationUseCase;

    @MockitoBean
    private GetNotificationBySourceEventUseCase
            getNotificationBySourceEventUseCase;

    @MockitoBean
    private GetUserNotificationsUseCase
            getUserNotificationsUseCase;

    @Test
    void shouldCreateNotification()
            throws Exception {

        Notification notification =
                notification(
                        UUID.randomUUID(),
                        NotificationStatus.SENT
                );

        when(createNotificationUseCase.create(
                any(CreateNotificationCommand.class)
        )).thenReturn(notification);

        mockMvc.perform(
                        post("/api/notifications")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "sourceEventId": "%s",
                                          "userId": "%s",
                                          "orderId": "%s",
                                          "channel": "EMAIL",
                                          "recipient": "test@example.com",
                                          "subject": "Order confirmed",
                                          "message": "Your order has been confirmed."
                                        }
                                        """.formatted(
                                        notification.getSourceEventId(),
                                        notification.getUserId(),
                                        notification.getOrderId()
                                ))
                )
                .andExpect(status().isCreated())
                .andExpect(
                        jsonPath("$.id")
                                .value(
                                        notification.getId()
                                                .toString()
                                )
                )
                .andExpect(
                        jsonPath("$.sourceEventId")
                                .value(
                                        notification
                                                .getSourceEventId()
                                                .toString()
                                )
                )
                .andExpect(
                        jsonPath("$.channel")
                                .value("EMAIL")
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("SENT")
                )
                .andExpect(
                        jsonPath("$.sentAt")
                                .exists()
                );
    }

    @Test
    void shouldRejectBlankRecipient()
            throws Exception {

        mockMvc.perform(
                        post("/api/notifications")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "sourceEventId": "%s",
                                          "userId": "%s",
                                          "channel": "EMAIL",
                                          "recipient": "",
                                          "subject": "Subject",
                                          "message": "Message"
                                        }
                                        """.formatted(
                                        UUID.randomUUID(),
                                        UUID.randomUUID()
                                ))
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.errors.recipient")
                                .exists()
                );
    }

    @Test
    void shouldRejectMissingSourceEventId()
            throws Exception {

        mockMvc.perform(
                        post("/api/notifications")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "userId": "%s",
                                          "channel": "EMAIL",
                                          "recipient": "test@example.com",
                                          "subject": "Subject",
                                          "message": "Message"
                                        }
                                        """.formatted(
                                        UUID.randomUUID()
                                ))
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath(
                                "$.errors.sourceEventId"
                        ).exists()
                );
    }

    @Test
    void shouldReturnBadRequestForUnknownChannel()
            throws Exception {

        mockMvc.perform(
                        post("/api/notifications")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "sourceEventId": "%s",
                                          "userId": "%s",
                                          "channel": "WHATSAPP",
                                          "recipient": "test@example.com",
                                          "subject": "Subject",
                                          "message": "Message"
                                        }
                                        """.formatted(
                                        UUID.randomUUID(),
                                        UUID.randomUUID()
                                ))
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.title")
                                .value(
                                        "Malformed request"
                                )
                );
    }

    @Test
    void shouldGetNotificationById()
            throws Exception {

        Notification notification =
                notification(
                        UUID.randomUUID(),
                        NotificationStatus.SENT
                );

        when(getNotificationUseCase.getById(
                notification.getId()
        )).thenReturn(notification);

        mockMvc.perform(
                        get(
                                "/api/notifications/{notificationId}",
                                notification.getId()
                        )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.id")
                                .value(
                                        notification
                                                .getId()
                                                .toString()
                                )
                );
    }

    @Test
    void shouldGetNotificationBySourceEvent()
            throws Exception {

        Notification notification =
                notification(
                        UUID.randomUUID(),
                        NotificationStatus.SENT
                );

        when(getNotificationBySourceEventUseCase
                .getBySourceEventId(
                        notification.getSourceEventId()
                ))
                .thenReturn(notification);

        mockMvc.perform(
                        get(
                                "/api/notifications/event/{sourceEventId}",
                                notification.getSourceEventId()
                        )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.sourceEventId")
                                .value(
                                        notification
                                                .getSourceEventId()
                                                .toString()
                                )
                );
    }

    @Test
    void shouldGetUserNotifications()
            throws Exception {

        UUID userId = UUID.randomUUID();

        Notification first =
                notification(
                        userId,
                        NotificationStatus.SENT
                );

        Notification second =
                notification(
                        userId,
                        NotificationStatus.FAILED
                );

        when(getUserNotificationsUseCase
                .getByUserId(userId))
                .thenReturn(
                        List.of(first, second)
                );

        mockMvc.perform(
                        get(
                                "/api/notifications/user/{userId}",
                                userId
                        )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.length()")
                                .value(2)
                )
                .andExpect(
                        jsonPath("$[0].status")
                                .value("SENT")
                )
                .andExpect(
                        jsonPath("$[1].status")
                                .value("FAILED")
                );
    }

    @Test
    void shouldReturnNotFound()
            throws Exception {

        UUID notificationId =
                UUID.randomUUID();

        when(getNotificationUseCase.getById(
                notificationId
        )).thenThrow(
                new NotificationNotFoundException(
                        notificationId
                )
        );

        mockMvc.perform(
                        get(
                                "/api/notifications/{notificationId}",
                                notificationId
                        )
                )
                .andExpect(status().isNotFound())
                .andExpect(
                        jsonPath("$.title")
                                .value(
                                        "Notification not found"
                                )
                );
    }

    @Test
    void shouldReturnConflictForDuplicateData()
            throws Exception {

        when(createNotificationUseCase.create(
                any(CreateNotificationCommand.class)
        )).thenThrow(
                new DataIntegrityViolationException(
                        "duplicate"
                )
        );

        mockMvc.perform(
                        post("/api/notifications")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "sourceEventId": "%s",
                                          "userId": "%s",
                                          "channel": "EMAIL",
                                          "recipient": "test@example.com",
                                          "subject": "Subject",
                                          "message": "Message"
                                        }
                                        """.formatted(
                                        UUID.randomUUID(),
                                        UUID.randomUUID()
                                ))
                )
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.status")
                                .value(409)
                );
    }

    private Notification notification(
            UUID userId,
            NotificationStatus status
    ) {
        Instant now = Instant.now();

        return new Notification(
                UUID.randomUUID(),
                UUID.randomUUID(),
                userId,
                UUID.randomUUID(),
                NotificationChannel.EMAIL,
                "test@example.com",
                "Order confirmed",
                "Your order has been confirmed.",
                status,
                now,
                status == NotificationStatus.SENT
                        ? now
                        : null
        );
    }
}