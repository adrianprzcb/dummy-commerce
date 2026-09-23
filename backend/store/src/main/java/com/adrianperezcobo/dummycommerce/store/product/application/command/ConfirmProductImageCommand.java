package com.adrianperezcobo.dummycommerce.store.product.application.command;

public record ConfirmProductImageCommand(
        String altText,
        int position,
        boolean primary
) {
}