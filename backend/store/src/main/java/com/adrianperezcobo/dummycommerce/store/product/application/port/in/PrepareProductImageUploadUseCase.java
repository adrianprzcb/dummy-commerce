package com.adrianperezcobo.dummycommerce.store.product.application.port.in;

import com.adrianperezcobo.dummycommerce.store.product.application.result.PreparedProductImageUpload;

import java.util.UUID;

public interface PrepareProductImageUploadUseCase {

    PreparedProductImageUpload prepare(UUID productId);
}