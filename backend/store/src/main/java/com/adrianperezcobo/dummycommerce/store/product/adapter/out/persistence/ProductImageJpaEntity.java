package com.adrianperezcobo.dummycommerce.store.product.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "product_images")
public class ProductImageJpaEntity {

    @Id
    private UUID id;

    @Column(name = "object_key", nullable = false, unique = true)
    private String objectKey;

    @Column(name = "alt_text", length = 255)
    private String altText;

    @Column(nullable = false)
    private int position;

    @Column(name = "is_primary", nullable = false)
    private boolean primary;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductJpaEntity product;

    protected ProductImageJpaEntity() {
    }

    public ProductImageJpaEntity(
            UUID id,
            String objectKey,
            String altText,
            int position,
            boolean primary
    ) {
        this.id = id;
        this.objectKey = objectKey;
        this.altText = altText;
        this.position = position;
        this.primary = primary;
    }

    void setProduct(ProductJpaEntity product) {
        this.product = product;
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