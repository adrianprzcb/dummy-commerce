package com.adrianperezcobo.dummycommerce.notifications.notification.adapter.in.web;

import com.adrianperezcobo.dummycommerce.notifications.notification.adapter.in.web.request.CreateNotificationRequest;
import com.adrianperezcobo.dummycommerce.notifications.notification.adapter.in.web.response.NotificationResponse;
import com.adrianperezcobo.dummycommerce.notifications.notification.application.port.in.CreateNotificationUseCase;
import com.adrianperezcobo.dummycommerce.notifications.notification.application.port.in.GetNotificationBySourceEventUseCase;
import com.adrianperezcobo.dummycommerce.notifications.notification.application.port.in.GetNotificationUseCase;
import com.adrianperezcobo.dummycommerce.notifications.notification.application.port.in.GetUserNotificationsUseCase;
import jakarta.validation.Valid;
import com.adrianperezcobo.dummycommerce.notifications.shared.security.AuthenticatedUser;
import com.adrianperezcobo.dummycommerce.notifications.notification.domain.Notification;
import com.adrianperezcobo.dummycommerce.notifications.notification.application.exception.NotificationNotFoundException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
            @PathVariable UUID notificationId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        Notification notification = getNotificationUseCase.getById(notificationId);
        requireOwner(notification, user);
        return mapper.toResponse(notification);
    }

    @GetMapping("/event/{sourceEventId}")
    public NotificationResponse getBySourceEventId(
            @PathVariable UUID sourceEventId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        Notification notification = getNotificationBySourceEventUseCase.getBySourceEventId(sourceEventId);
        requireOwner(notification, user);
        return mapper.toResponse(notification);
    }

    @GetMapping("/me")
    public List<NotificationResponse> getMine(@AuthenticationPrincipal AuthenticatedUser user) {
        return getUserNotificationsUseCase.getByUserId(user.userId()).stream().map(mapper::toResponse).toList();
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
    private void requireOwner(Notification notification, AuthenticatedUser user) {
        if (!user.isAdmin() && !user.userId().equals(notification.getUserId())) {
            throw new NotificationNotFoundException(notification.getId());
        }
    }
}
