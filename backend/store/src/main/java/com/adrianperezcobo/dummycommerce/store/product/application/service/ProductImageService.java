package com.adrianperezcobo.dummycommerce.store.product.application.service;

import com.adrianperezcobo.dummycommerce.store.product.application.command.ConfirmProductImageCommand;
import com.adrianperezcobo.dummycommerce.store.product.application.exception.ProductImageNotUploadedException;
import com.adrianperezcobo.dummycommerce.store.product.application.exception.ProductNotFoundException;
import com.adrianperezcobo.dummycommerce.store.product.application.port.in.*;
import com.adrianperezcobo.dummycommerce.store.product.application.port.out.ProductImageStoragePort;
import com.adrianperezcobo.dummycommerce.store.product.application.port.out.ProductRepository;
import com.adrianperezcobo.dummycommerce.store.product.application.result.PreparedProductImageUpload;
import com.adrianperezcobo.dummycommerce.store.product.domain.Product;
import com.adrianperezcobo.dummycommerce.store.product.domain.ProductImage;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ProductImageService
        implements PrepareProductImageUploadUseCase,
        ConfirmProductImageUseCase,
        RemoveProductImageUseCase,
        SetPrimaryProductImageUseCase,
        GetProductImageContentUseCase {

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

    @Override
    public Product confirm(UUID productId, UUID imageId, ConfirmProductImageCommand command) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        String objectKey =
                "products/" + productId + "/" + imageId;

        if (!imageStorage.exists(objectKey)) {
            throw new ProductImageNotUploadedException(imageId);
        }

        ProductImage image = new ProductImage(
                imageId,
                objectKey,
                command.altText(),
                command.position(),
                command.primary()
        );

        product.addImage(image);

        return productRepository.save(product);
    }

    @Override
    public void remove(UUID productId, UUID imageId) {
        Product product = getExistingProduct(productId);

        ProductImage removedImage = product.removeImage(imageId);

        productRepository.save(product);

        imageStorage.delete(removedImage.getObjectKey());
    }

    @Override
    public Product setPrimary(UUID productId, UUID imageId) {
        Product product = getExistingProduct(productId);

        product.setPrimaryImage(imageId);

        return productRepository.save(product);
    }

    private Product getExistingProduct(UUID productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
    }

    @Override
    public String getAccessUrl(UUID productId, UUID imageId) {
        Product product = getExistingProduct(productId);

        String objectKey = product.getImageObjectKey(imageId);

        return imageStorage.createReadUrl(objectKey);
    }
}