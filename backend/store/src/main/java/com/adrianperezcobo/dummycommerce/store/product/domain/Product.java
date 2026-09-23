package com.adrianperezcobo.dummycommerce.store.product.domain;

import com.adrianperezcobo.dummycommerce.store.product.domain.exception.InvalidProductStateException;
import com.adrianperezcobo.dummycommerce.store.product.domain.exception.ProductImageNotFoundException;

import java.math.BigDecimal;
import java.util.*;

public class Product {

    private final UUID id;

    private String name;
    private String description;
    private BigDecimal price;
    private ProductStatus status;
    private UUID categoryId;

    private final List<ProductImage> images;

    public Product(
            UUID id,
            String name,
            String description,
            BigDecimal price,
            ProductStatus status,
            UUID categoryId
    ) {
        this.id = Objects.requireNonNull(id);

        changeName(name);
        changePrice(price);

        this.description = description;
        this.status = Objects.requireNonNull(status);
        this.categoryId = Objects.requireNonNull(categoryId);

        this.images = new ArrayList<>();
    }

    public void changeName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Product name cannot be empty");
        }

        this.name = name;
    }

    public void changePrice(BigDecimal price) {
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Product price cannot be negative");
        }

        this.price = price;
    }

    public void changeDescription(String description) {
        this.description = description;
    }

    public void changeCategory(UUID categoryId) {
        this.categoryId = Objects.requireNonNull(
                categoryId,
                "Category id cannot be null"
        );
    }

    public void addImage(ProductImage image) {
        Objects.requireNonNull(image, "Product image cannot be null");

        boolean duplicatedId = images.stream()
                .anyMatch(existing -> existing.getId().equals(image.getId()));

        if (duplicatedId) {
            throw new IllegalArgumentException(
                    "Product image already exists: " + image.getId()
            );
        }

        boolean duplicatedPosition = images.stream()
                .anyMatch(existing ->
                        existing.getPosition() == image.getPosition()
                );

        if (duplicatedPosition) {
            throw new IllegalArgumentException(
                    "Image position already exists: " + image.getPosition()
            );
        }

        if (images.isEmpty()) {
            image.makePrimary();
        } else if (image.isPrimary()) {
            images.forEach(ProductImage::removePrimary);
        }

        images.add(image);
    }

    public void setPrimaryImage(UUID imageId) {
        ProductImage selectedImage = getImage(imageId);

        images.forEach(ProductImage::removePrimary);
        selectedImage.makePrimary();
    }

    public ProductImage removeImage(UUID imageId) {
        ProductImage image = getImage(imageId);

        boolean wasPrimary = image.isPrimary();

        images.remove(image);

        if (wasPrimary && !images.isEmpty()) {
            images.stream()
                    .min(Comparator.comparingInt(ProductImage::getPosition))
                    .orElseThrow()
                    .makePrimary();
        }

        return image;
    }

    private ProductImage getImage(UUID imageId) {
        return images.stream()
                .filter(image -> image.getId().equals(imageId))
                .findFirst()
                .orElseThrow(() ->
                        new ProductImageNotFoundException(imageId)
                );
    }

    public void activate() {
        if (status == ProductStatus.DISCONTINUED) {
            throw new InvalidProductStateException(
                    "A discontinued product cannot be activated"
            );
        }

        this.status = ProductStatus.ACTIVE;
    }

    public void deactivate() {
        if (status == ProductStatus.DISCONTINUED) {
            throw new InvalidProductStateException(
                    "A discontinued product cannot be deactivated"
            );
        }

        this.status = ProductStatus.INACTIVE;
    }


    public String getImageObjectKey(UUID imageId) {
        return getImage(imageId).getObjectKey();
    }



    public void discontinue() {
        this.status = ProductStatus.DISCONTINUED;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public ProductStatus getStatus() {
        return status;
    }

    public UUID getCategoryId() {
        return categoryId;
    }

    public List<ProductImage> getImages() {
        return Collections.unmodifiableList(images);
    }
}