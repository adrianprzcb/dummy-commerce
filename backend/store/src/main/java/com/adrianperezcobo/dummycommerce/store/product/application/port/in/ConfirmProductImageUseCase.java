package com.adrianperezcobo.dummycommerce.store.product.application.port.in;

import com.adrianperezcobo.dummycommerce.store.product.application.command.ConfirmProductImageCommand;
import com.adrianperezcobo.dummycommerce.store.product.domain.Product;

import java.util.UUID;

public interface ConfirmProductImageUseCase {

    Product confirm(
            UUID productId,
            UUID imageId,
            ConfirmProductImageCommand command
    );
}