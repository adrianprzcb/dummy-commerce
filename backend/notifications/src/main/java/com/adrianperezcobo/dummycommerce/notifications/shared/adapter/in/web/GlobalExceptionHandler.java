package com.adrianperezcobo.dummycommerce.notifications.shared.adapter.in.web;

import com.adrianperezcobo.dummycommerce.notifications.notification.application.exception.NotificationForSourceEventNotFoundException;
import com.adrianperezcobo.dummycommerce.notifications.notification.application.exception.NotificationNotFoundException;
import com.adrianperezcobo.dummycommerce.notifications.notification.domain.exception.InvalidNotificationStateException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(
            NotificationNotFoundException.class
    )
    public ProblemDetail handleNotificationNotFound(
            NotificationNotFoundException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.NOT_FOUND,
                "Notification not found",
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(
            NotificationForSourceEventNotFoundException.class
    )
    public ProblemDetail handleNotificationForEventNotFound(
            NotificationForSourceEventNotFoundException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.NOT_FOUND,
                "Notification not found",
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(
            InvalidNotificationStateException.class
    )
    public ProblemDetail handleInvalidState(
            InvalidNotificationStateException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.CONFLICT,
                "Invalid notification state",
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(
            DataIntegrityViolationException.class
    )
    public ProblemDetail handleDataIntegrityViolation(
            DataIntegrityViolationException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.CONFLICT,
                "Notification conflict",
                "Notification conflicts with existing data",
                request
        );
    }

    @ExceptionHandler(
            IllegalArgumentException.class
    )
    public ProblemDetail handleIllegalArgument(
            IllegalArgumentException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.BAD_REQUEST,
                "Invalid request",
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(
            HttpMessageNotReadableException.class
    )
    public ProblemDetail handleMalformedRequest(
            HttpMessageNotReadableException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.BAD_REQUEST,
                "Malformed request",
                "Request body could not be read",
                request
        );
    }

    @ExceptionHandler(
            MethodArgumentNotValidException.class
    )
    public ProblemDetail handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        ProblemDetail problem = problem(
                HttpStatus.BAD_REQUEST,
                "Validation error",
                "Request validation failed",
                request
        );

        Map<String, String> errors =
                new LinkedHashMap<>();

        exception.getBindingResult()
                .getFieldErrors()
                .forEach(error ->
                        errors.put(
                                error.getField(),
                                error.getDefaultMessage()
                        )
                );

        problem.setProperty(
                "errors",
                errors
        );

        return problem;
    }

    private ProblemDetail problem(
            HttpStatus status,
            String title,
            String detail,
            HttpServletRequest request
    ) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(
                        status,
                        detail
                );

        problem.setTitle(title);

        problem.setInstance(
                URI.create(
                        request.getRequestURI()
                )
        );

        return problem;
    }
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception exception, HttpServletRequest request) {
        if (exception instanceof org.springframework.web.ErrorResponse response) {
            return response.getBody();
        }
        org.slf4j.LoggerFactory.getLogger(getClass()).error("Unhandled request failure", exception);
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Request could not be completed");
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }

    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleInvalidParameter(Exception exception, HttpServletRequest request) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request parameter is invalid");
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }

}