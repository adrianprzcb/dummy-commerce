package com.adrianperezcobo.dummycommerce.store.product.adapter.in.web.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ConfirmProductImageRequest(

        @Size(max = 255)
        String altText,

        @NotNull
        @PositiveOrZero
        Integer position,

        boolean primary
) {
}