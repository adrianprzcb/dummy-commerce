package com.adrianperezcobo.dummycommerce.store.product.adapter.out.persistence;

import com.adrianperezcobo.dummycommerce.store.category.adapter.out.persistence.CategoryJpaEntity;
import com.adrianperezcobo.dummycommerce.store.product.domain.Product;
import com.adrianperezcobo.dummycommerce.store.product.domain.ProductImage;
import org.springframework.stereotype.Component;

@Component
public class ProductPersistenceMapper {

    public ProductJpaEntity toEntity(
            Product product,
            CategoryJpaEntity category
    ) {
        ProductJpaEntity entity = new ProductJpaEntity(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStatus(),
                category
        );

        product.getImages()
                .stream()
                .map(this::toImageEntity)
                .forEach(entity::addImage);

        return entity;
    }

    public Product toDomain(ProductJpaEntity entity) {

        Product product = new Product(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getPrice(),
                entity.getStatus(),
                entity.getCategory().getId()
        );

        entity.getImages()
                .stream()
                .map(this::toImageDomain)
                .forEach(product::addImage);

        return product;
    }

    private ProductImageJpaEntity toImageEntity(ProductImage image) {
        return new ProductImageJpaEntity(
                image.getId(),
                image.getObjectKey(),
                image.getAltText(),
                image.getPosition(),
                image.isPrimary()
        );
    }

    private ProductImage toImageDomain(ProductImageJpaEntity entity) {
        return new ProductImage(
                entity.getId(),
                entity.getObjectKey(),
                entity.getAltText(),
                entity.getPosition(),
                entity.isPrimary()
        );
    }
}