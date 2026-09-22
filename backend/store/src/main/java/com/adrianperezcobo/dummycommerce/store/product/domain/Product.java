package com.adrianperezcobo.dummycommerce.store.product.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

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
        Objects.requireNonNull(image);

        if (image.isPrimary()) {
            images.forEach(ProductImage::removePrimary);
        }

        images.add(image);
    }

    public void setPrimaryImage(UUID imageId) {
        ProductImage selectedImage = images.stream()
                .filter(image -> image.getId().equals(imageId))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException("Product image not found")
                );

        images.forEach(ProductImage::removePrimary);
        selectedImage.makePrimary();
    }

    public void discontinue() {
        this.status = ProductStatus.DISCONTINUED;
    }

    public void activate() {
        this.status = ProductStatus.ACTIVE;
    }

    public void deactivate() {
        this.status = ProductStatus.INACTIVE;
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