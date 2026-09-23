package com.adrianperezcobo.dummycommerce.store.product.adapter.out.storage.minio;

import com.adrianperezcobo.dummycommerce.store.product.application.port.out.ProductImageStoragePort;
import io.minio.*;
import io.minio.Http;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Component
public class MinioProductImageStorageAdapter
        implements ProductImageStoragePort {

    private final MinioClient minioClient;
    private final MinioProperties properties;

    public MinioProductImageStorageAdapter(
            MinioClient minioClient,
            MinioProperties properties
    ) {
        this.minioClient = minioClient;
        this.properties = properties;
    }

    @PostConstruct
    void initializeBucket() {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(properties.bucket())
                            .build()
            );

            if (!exists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder()
                                .bucket(properties.bucket())
                                .build()
                );
            }

        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Could not initialize product image bucket",
                    exception
            );
        }
    }

    @Override
    public String createUploadUrl(String objectKey) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Http.Method.PUT)
                            .bucket(properties.bucket())
                            .object(objectKey)
                            .expiry(
                                    properties.uploadUrlExpirationSeconds(),
                                    TimeUnit.SECONDS
                            )
                            .build()
            );

        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Could not create image upload URL",
                    exception
            );
        }
    }

    @Override
    public String createReadUrl(String objectKey) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Http.Method.GET)
                            .bucket(properties.bucket())
                            .object(objectKey)
                            .expiry(
                                    properties.uploadUrlExpirationSeconds(),
                                    TimeUnit.SECONDS
                            )
                            .build()
            );

        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Could not create image read URL",
                    exception
            );
        }
    }


    @Override
    public boolean exists(String objectKey) {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(properties.bucket())
                            .object(objectKey)
                            .build()
            );

            return true;

        } catch (Exception exception) {
            return false;
        }
    }

    @Override
    public void delete(String objectKey) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(properties.bucket())
                            .object(objectKey)
                            .build()
            );

        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Could not delete product image",
                    exception
            );
        }
    }
}