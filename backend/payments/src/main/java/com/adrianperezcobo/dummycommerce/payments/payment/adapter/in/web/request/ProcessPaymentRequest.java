package com.adrianperezcobo.dummycommerce.payments.payment.adapter.in.web.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.util.UUID;

public record ProcessPaymentRequest(

        @NotNull
        UUID orderId,

        @NotNull
        @DecimalMin(
                value = "0.00",
                inclusive = false
        )
        @Digits(
                integer = 17,
                fraction = 2
        )
        BigDecimal amount,

        @NotBlank
        @Pattern(
                regexp = "^[A-Za-z]{3}$"
        )
        String currency
) {
}