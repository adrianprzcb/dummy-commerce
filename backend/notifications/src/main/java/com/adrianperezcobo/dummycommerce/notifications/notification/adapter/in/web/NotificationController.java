package com.adrianperezcobo.dummycommerce.notifications.notification.adapter.in.web;

import com.adrianperezcobo.dummycommerce.notifications.notification.adapter.in.web.request.CreateNotificationRequest;
import com.adrianperezcobo.dummycommerce.notifications.notification.adapter.in.web.response.NotificationResponse;
import com.adrianperezcobo.dummycommerce.notifications.notification.application.port.in.CreateNotificationUseCase;
import com.adrianperezcobo.dummycommerce.notifications.notification.application.port.in.GetNotificationBySourceEventUseCase;
import com.adrianperezcobo.dummycommerce.notifications.notification.application.port.in.GetNotificationUseCase;
import com.adrianperezcobo.dummycommerce.notifications.notification.application.port.in.GetUserNotificationsUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final CreateNotificationUseCase createNotificationUseCase;
    private final GetNotificationUseCase getNotificationUseCase;
    private final GetNotificationBySourceEventUseCase
            getNotificationBySourceEventUseCase;
    private final GetUserNotificationsUseCase
            getUserNotificationsUseCase;
    private final NotificationWebMapper mapper;

    public NotificationController(
            CreateNotificationUseCase createNotificationUseCase,
            GetNotificationUseCase getNotificationUseCase,
            GetNotificationBySourceEventUseCase
                    getNotificationBySourceEventUseCase,
            GetUserNotificationsUseCase
                    getUserNotificationsUseCase,
            NotificationWebMapper mapper
    ) {
        this.createNotificationUseCase =
                createNotificationUseCase;

        this.getNotificationUseCase =
                getNotificationUseCase;

        this.getNotificationBySourceEventUseCase =
                getNotificationBySourceEventUseCase;

        this.getUserNotificationsUseCase =
                getUserNotificationsUseCase;

        this.mapper = mapper;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public NotificationResponse create(
            @Valid
            @RequestBody
            CreateNotificationRequest request
    ) {
        return mapper.toResponse(
                createNotificationUseCase.create(
                        mapper.toCommand(request)
                )
        );
    }

    @GetMapping("/{notificationId}")
    public NotificationResponse getById(
            @PathVariable UUID notificationId
    ) {
        return mapper.toResponse(
                getNotificationUseCase.getById(
                        notificationId
                )
        );
    }

    @GetMapping("/event/{sourceEventId}")
    public NotificationResponse getBySourceEventId(
            @PathVariable UUID sourceEventId
    ) {
        return mapper.toResponse(
                getNotificationBySourceEventUseCase
                        .getBySourceEventId(
                                sourceEventId
                        )
        );
    }

    @GetMapping("/user/{userId}")
    public List<NotificationResponse> getByUserId(
            @PathVariable UUID userId
    ) {
        return getUserNotificationsUseCase
                .getByUserId(userId)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }
}