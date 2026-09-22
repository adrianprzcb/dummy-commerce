package com.adrianperezcobo.dummycommerce.store.product.domain;

import java.util.Objects;
import java.util.UUID;

public class ProductImage {

    private final UUID id;
    private final String objectKey;
    private String altText;
    private int position;
    private boolean primary;

    public ProductImage(
            UUID id,
            String objectKey,
            String altText,
            int position,
            boolean primary
    ) {
        this.id = Objects.requireNonNull(id, "Image id cannot be null");

        if (objectKey == null || objectKey.isBlank()) {
            throw new IllegalArgumentException("Object key cannot be empty");
        }

        if (position < 0) {
            throw new IllegalArgumentException("Position cannot be negative");
        }

        this.objectKey = objectKey;
        this.altText = altText;
        this.position = position;
        this.primary = primary;
    }

    public void makePrimary() {
        this.primary = true;
    }

    public void removePrimary() {
        this.primary = false;
    }

    public void changePosition(int position) {
        if (position < 0) {
            throw new IllegalArgumentException("Position cannot be negative");
        }

        this.position = position;
    }

    public UUID getId() {
        return id;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public String getAltText() {
        return altText;
    }

    public int getPosition() {
        return position;
    }

    public boolean isPrimary() {
        return primary;
    }
}