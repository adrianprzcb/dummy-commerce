package com.adrianperezcobo.dummycommerce.inventory.shared.adapter.in.web;

import com.adrianperezcobo.dummycommerce.inventory.reservation.application.exception.ReservationAlreadyExistsException;
import com.adrianperezcobo.dummycommerce.inventory.reservation.application.exception.ReservationNotFoundException;
import com.adrianperezcobo.dummycommerce.inventory.reservation.domain.exception.InvalidReservationStateException;
import com.adrianperezcobo.dummycommerce.inventory.stock.application.exception.InventoryItemAlreadyExistsException;
import com.adrianperezcobo.dummycommerce.inventory.stock.application.exception.InventoryItemNotFoundException;
import com.adrianperezcobo.dummycommerce.inventory.stock.domain.exception.InsufficientStockException;
import com.adrianperezcobo.dummycommerce.inventory.stock.domain.exception.InvalidStockStateException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InventoryItemNotFoundException.class)
    public ProblemDetail handleInventoryNotFound(
            InventoryItemNotFoundException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.NOT_FOUND,
                "Inventory item not found",
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(ReservationNotFoundException.class)
    public ProblemDetail handleReservationNotFound(
            ReservationNotFoundException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.NOT_FOUND,
                "Reservation not found",
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(InventoryItemAlreadyExistsException.class)
    public ProblemDetail handleInventoryAlreadyExists(
            InventoryItemAlreadyExistsException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.CONFLICT,
                "Inventory item already exists",
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(ReservationAlreadyExistsException.class)
    public ProblemDetail handleReservationAlreadyExists(
            ReservationAlreadyExistsException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.CONFLICT,
                "Reservation already exists",
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ProblemDetail handleInsufficientStock(
            InsufficientStockException exception,
            HttpServletRequest request
    ) {
        ProblemDetail problem = problem(
                HttpStatus.CONFLICT,
                "Insufficient stock",
                exception.getMessage(),
                request
        );

        problem.setProperty(
                "productId",
                exception.getProductId()
        );

        problem.setProperty(
                "requested",
                exception.getRequested()
        );

        problem.setProperty(
                "available",
                exception.getAvailable()
        );

        return problem;
    }

    @ExceptionHandler({
            InvalidStockStateException.class,
            InvalidReservationStateException.class
    })
    public ProblemDetail handleInvalidState(
            RuntimeException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.CONFLICT,
                "Invalid state",
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
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

    @ExceptionHandler(MethodArgumentNotValidException.class)
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
}