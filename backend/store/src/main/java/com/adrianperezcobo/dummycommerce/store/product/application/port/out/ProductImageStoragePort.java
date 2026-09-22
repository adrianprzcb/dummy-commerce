package com.adrianperezcobo.dummycommerce.store.product.application.port.out;

import java.time.Duration;

public interface ProductImageStoragePort {

    String createUploadUrl(
            String objectKey,
            Duration expiration
    );

    boolean exists(String objectKey);

    void delete(String objectKey);
}