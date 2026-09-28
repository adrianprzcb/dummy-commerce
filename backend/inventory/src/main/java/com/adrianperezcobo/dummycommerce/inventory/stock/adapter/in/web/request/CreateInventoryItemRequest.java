package com.adrianperezcobo.dummycommerce.inventory.stock.adapter.in.web.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.UUID;

public record CreateInventoryItemRequest(

        @NotNull
        UUID productId,

        @PositiveOrZero
        int initialQuantity
) {
}