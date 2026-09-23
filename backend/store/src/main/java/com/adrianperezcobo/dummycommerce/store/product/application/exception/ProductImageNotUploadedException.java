package com.adrianperezcobo.dummycommerce.store.product.application.exception;

import java.util.UUID;

public class ProductImageNotUploadedException extends RuntimeException {

    public ProductImageNotUploadedException(UUID imageId) {
        super("Product image has not been uploaded: " + imageId);
    }
}