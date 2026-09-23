package com.adrianperezcobo.dummycommerce.store.product.application.service;

import com.adrianperezcobo.dummycommerce.store.product.application.exception.ProductNotFoundException;
import com.adrianperezcobo.dummycommerce.store.product.application.port.in.PrepareProductImageUploadUseCase;
import com.adrianperezcobo.dummycommerce.store.product.application.port.out.ProductImageStoragePort;
import com.adrianperezcobo.dummycommerce.store.product.application.port.out.ProductRepository;
import com.adrianperezcobo.dummycommerce.store.product.application.result.PreparedProductImageUpload;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ProductImageService
        implements PrepareProductImageUploadUseCase {

    private final ProductRepository productRepository;
    private final ProductImageStoragePort imageStorage;

    public ProductImageService(
            ProductRepository productRepository,
            ProductImageStoragePort imageStorage
    ) {
        this.productRepository = productRepository;
        this.imageStorage = imageStorage;
    }

    @Override
    public PreparedProductImageUpload prepare(UUID productId) {

        if (!productRepository.existsById(productId)) {
            throw new ProductNotFoundException(productId);
        }

        UUID imageId = UUID.randomUUID();

        String objectKey =
                "products/" + productId + "/" + imageId;

        String uploadUrl = imageStorage.createUploadUrl(objectKey);

        return new PreparedProductImageUpload(
                imageId,
                uploadUrl
        );
    }
}