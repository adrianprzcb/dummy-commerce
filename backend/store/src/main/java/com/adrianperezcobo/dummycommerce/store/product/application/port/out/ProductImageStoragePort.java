package com.adrianperezcobo.dummycommerce.store.product.application.port.out;

public interface ProductImageStoragePort {

    String createUploadUrl(String objectKey);

    boolean exists(String objectKey);

    void delete(String objectKey);
}