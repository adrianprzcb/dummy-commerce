package com.adrianperezcobo.dummycommerce.store.product.adapter.out.storage.minio;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "storage.minio")
public record MinioProperties(
        String endpoint,
        String publicEndpoint,
        String accessKey,
        String secretKey,
        String bucket,
        int uploadUrlExpirationSeconds
) {
}