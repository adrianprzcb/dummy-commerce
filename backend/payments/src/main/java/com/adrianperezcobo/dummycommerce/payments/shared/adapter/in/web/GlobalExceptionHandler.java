package com.adrianperezcobo.dummycommerce.payments.shared.adapter.in.web;

import com.adrianperezcobo.dummycommerce.payments.payment.application.exception.PaymentForOrderNotFoundException;
import com.adrianperezcobo.dummycommerce.payments.payment.application.exception.PaymentNotFoundException;
import com.adrianperezcobo.dummycommerce.payments.payment.application.exception.PaymentRefundFailedException;
import com.adrianperezcobo.dummycommerce.payments.payment.domain.exception.InvalidPaymentStateException;
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

    @ExceptionHandler(PaymentNotFoundException.class)
    public ProblemDetail handlePaymentNotFound(
            PaymentNotFoundException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.NOT_FOUND,
                "Payment not found",
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(
            PaymentForOrderNotFoundException.class
    )
    public ProblemDetail handlePaymentForOrderNotFound(
            PaymentForOrderNotFoundException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.NOT_FOUND,
                "Payment not found",
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(
            InvalidPaymentStateException.class
    )
    public ProblemDetail handleInvalidPaymentState(
            InvalidPaymentStateException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.CONFLICT,
                "Invalid payment state",
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(
            PaymentRefundFailedException.class
    )
    public ProblemDetail handleRefundFailed(
            PaymentRefundFailedException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.BAD_GATEWAY,
                "Payment refund failed",
                exception.getMessage(),
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
}