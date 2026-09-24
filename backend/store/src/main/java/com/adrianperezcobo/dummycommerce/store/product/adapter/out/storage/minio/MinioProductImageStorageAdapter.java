package com.adrianperezcobo.dummycommerce.store.product.adapter.out.storage.minio;

import com.adrianperezcobo.dummycommerce.store.product.application.port.out.ProductImageStoragePort;
import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.Http.*;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class MinioProductImageStorageAdapter
        implements ProductImageStoragePort {

    private final MinioClient internalClient;
    private final MinioClient publicClient;
    private final MinioProperties properties;

    public MinioProductImageStorageAdapter(
            @Qualifier("minioInternalClient")
            MinioClient internalClient,

            @Qualifier("minioPublicClient")
            MinioClient publicClient,

            MinioProperties properties
    ) {
        this.internalClient = internalClient;
        this.publicClient = publicClient;
        this.properties = properties;
    }

    @PostConstruct
    void initializeBucket() {
        try {
            boolean exists = internalClient.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(properties.bucket())
                            .build()
            );

            if (!exists) {
                internalClient.makeBucket(
                        MakeBucketArgs.builder()
                                .bucket(properties.bucket())
                                .build()
                );
            }

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Could not initialize MinIO bucket",
                    e
            );
        }
    }

    @Override
    public String createUploadUrl(String objectKey) {
        try {
            return publicClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.PUT)
                            .bucket(properties.bucket())
                            .object(objectKey)
                            .expiry(
                                    properties.uploadUrlExpirationSeconds(),
                                    TimeUnit.SECONDS
                            )
                            .build()
            );

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Could not create MinIO upload URL",
                    e
            );
        }
    }

    @Override
    public String createReadUrl(String objectKey) {
        try {
            return publicClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(properties.bucket())
                            .object(objectKey)
                            .expiry(
                                    properties.uploadUrlExpirationSeconds(),
                                    TimeUnit.SECONDS
                            )
                            .build()
            );

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Could not create MinIO read URL",
                    e
            );
        }
    }

    @Override
    public boolean exists(String objectKey) {
        try {
            internalClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(properties.bucket())
                            .object(objectKey)
                            .build()
            );

            return true;

        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void delete(String objectKey) {
        try {
            internalClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(properties.bucket())
                            .object(objectKey)
                            .build()
            );

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Could not delete MinIO object",
                    e
            );
        }
    }
}