package com.adrianperezcobo.dummycommerce.inventory.reservation.adapter.in.web.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record ReserveStockRequest(

        @NotNull
        UUID orderId,

        @NotNull
        UUID productId,

        @Positive
        int quantity
) {
}