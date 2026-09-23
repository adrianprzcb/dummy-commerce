package com.adrianperezcobo.dummycommerce.store.product.application.service;

import com.adrianperezcobo.dummycommerce.store.product.application.command.ConfirmProductImageCommand;
import com.adrianperezcobo.dummycommerce.store.product.application.exception.ProductImageNotUploadedException;
import com.adrianperezcobo.dummycommerce.store.product.application.exception.ProductNotFoundException;
import com.adrianperezcobo.dummycommerce.store.product.application.port.out.ProductImageStoragePort;
import com.adrianperezcobo.dummycommerce.store.product.application.port.out.ProductRepository;
import com.adrianperezcobo.dummycommerce.store.product.application.result.PreparedProductImageUpload;
import com.adrianperezcobo.dummycommerce.store.product.domain.Product;
import com.adrianperezcobo.dummycommerce.store.product.domain.ProductImage;
import com.adrianperezcobo.dummycommerce.store.product.domain.ProductStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductImageServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductImageStoragePort imageStorage;

    @InjectMocks
    private ProductImageService productImageService;

    @Test
    void shouldPrepareImageUpload() {
        UUID productId = UUID.randomUUID();

        when(productRepository.existsById(productId))
                .thenReturn(true);

        when(imageStorage.createUploadUrl(any()))
                .thenReturn("http://minio/upload");

        PreparedProductImageUpload result =
                productImageService.prepare(productId);

        assertThat(result.imageId()).isNotNull();
        assertThat(result.uploadUrl())
                .isEqualTo("http://minio/upload");

        verify(imageStorage).createUploadUrl(
                startsWith("products/" + productId + "/")
        );
    }

    @Test
    void shouldRejectUploadPreparationForUnknownProduct() {
        UUID productId = UUID.randomUUID();

        when(productRepository.existsById(productId))
                .thenReturn(false);

        assertThatThrownBy(
                () -> productImageService.prepare(productId)
        ).isInstanceOf(ProductNotFoundException.class);

        verifyNoInteractions(imageStorage);
    }

    @Test
    void shouldConfirmUploadedImage() {
        Product product = createProduct();

        UUID imageId = UUID.randomUUID();

        String objectKey =
                "products/" + product.getId() + "/" + imageId;

        when(productRepository.findById(product.getId()))
                .thenReturn(Optional.of(product));

        when(imageStorage.exists(objectKey))
                .thenReturn(true);

        when(productRepository.save(product))
                .thenReturn(product);

        ConfirmProductImageCommand command =
                new ConfirmProductImageCommand(
                        "Front view",
                        0,
                        false
                );

        Product result = productImageService.confirm(
                product.getId(),
                imageId,
                command
        );

        assertThat(result.getImages()).hasSize(1);

        ProductImage image = result.getImages().getFirst();

        assertThat(image.getId()).isEqualTo(imageId);
        assertThat(image.getObjectKey()).isEqualTo(objectKey);

        // primera imagen => primary automáticamente
        assertThat(image.isPrimary()).isTrue();
    }

    @Test
    void shouldRejectConfirmationWhenObjectWasNotUploaded() {
        Product product = createProduct();

        UUID imageId = UUID.randomUUID();

        String objectKey =
                "products/" + product.getId() + "/" + imageId;

        when(productRepository.findById(product.getId()))
                .thenReturn(Optional.of(product));

        when(imageStorage.exists(objectKey))
                .thenReturn(false);

        ConfirmProductImageCommand command =
                new ConfirmProductImageCommand(
                        "Front view",
                        0,
                        true
                );

        assertThatThrownBy(() ->
                productImageService.confirm(
                        product.getId(),
                        imageId,
                        command
                )
        ).isInstanceOf(ProductImageNotUploadedException.class);

        verify(productRepository, never()).save(any());
    }

    @Test
    void shouldDeleteImageFromProductAndStorage() {
        Product product = createProduct();

        ProductImage image = new ProductImage(
                UUID.randomUUID(),
                "products/test/image",
                "Front",
                0,
                true
        );

        product.addImage(image);

        when(productRepository.findById(product.getId()))
                .thenReturn(Optional.of(product));

        when(productRepository.save(product))
                .thenReturn(product);

        productImageService.remove(
                product.getId(),
                image.getId()
        );

        assertThat(product.getImages()).isEmpty();

        verify(imageStorage)
                .delete(image.getObjectKey());
    }

    @Test
    void shouldChangePrimaryImage() {
        Product product = createProduct();

        ProductImage first = new ProductImage(
                UUID.randomUUID(),
                "products/test/first",
                "Front",
                0,
                true
        );

        ProductImage second = new ProductImage(
                UUID.randomUUID(),
                "products/test/second",
                "Back",
                1,
                false
        );

        product.addImage(first);
        product.addImage(second);

        when(productRepository.findById(product.getId()))
                .thenReturn(Optional.of(product));

        when(productRepository.save(product))
                .thenReturn(product);

        Product result = productImageService.setPrimary(
                product.getId(),
                second.getId()
        );

        assertThat(result.getImages())
                .filteredOn(ProductImage::isPrimary)
                .containsExactly(second);
    }

    private Product createProduct() {
        return new Product(
                UUID.randomUUID(),
                "Keyboard",
                "Mechanical keyboard",
                new BigDecimal("99.99"),
                ProductStatus.ACTIVE,
                UUID.randomUUID()
        );
    }
}