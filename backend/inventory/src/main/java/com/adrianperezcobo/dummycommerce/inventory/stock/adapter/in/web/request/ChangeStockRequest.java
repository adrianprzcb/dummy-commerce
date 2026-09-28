package com.adrianperezcobo.dummycommerce.inventory.stock.adapter.in.web.request;

import jakarta.validation.constraints.Positive;

public record ChangeStockRequest(

        @Positive
        int quantity
) {
}