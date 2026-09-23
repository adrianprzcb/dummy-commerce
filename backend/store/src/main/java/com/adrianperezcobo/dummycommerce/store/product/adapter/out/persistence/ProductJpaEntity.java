package com.adrianperezcobo.dummycommerce.store.product.adapter.out.persistence;

import com.adrianperezcobo.dummycommerce.store.category.adapter.out.persistence.CategoryJpaEntity;
import com.adrianperezcobo.dummycommerce.store.product.domain.ProductStatus;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "products")
public class ProductJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ProductStatus status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private CategoryJpaEntity category;

    @OneToMany(
            mappedBy = "product",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @OrderBy("position ASC")
    private List<ProductImageJpaEntity> images = new ArrayList<>();

    protected ProductJpaEntity() {
    }

    public ProductJpaEntity(
            UUID id,
            String name,
            String description,
            BigDecimal price,
            ProductStatus status,
            CategoryJpaEntity category
    ) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.status = status;
        this.category = category;
    }

    public void addImage(ProductImageJpaEntity image) {
        images.add(image);
        image.setProduct(this);
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

    public CategoryJpaEntity getCategory() {
        return category;
    }

    public List<ProductImageJpaEntity> getImages() {
        return images;
    }
}