package com.adrianperezcobo.dummycommerce.store.product.adapter.in.web;

import com.adrianperezcobo.dummycommerce.store.product.adapter.in.web.request.CreateProductRequest;
import com.adrianperezcobo.dummycommerce.store.product.adapter.in.web.request.UpdateProductRequest;
import com.adrianperezcobo.dummycommerce.store.product.adapter.in.web.response.ProductImageResponse;
import com.adrianperezcobo.dummycommerce.store.product.adapter.in.web.response.ProductResponse;
import com.adrianperezcobo.dummycommerce.store.product.application.command.CreateProductCommand;
import com.adrianperezcobo.dummycommerce.store.product.application.command.UpdateProductCommand;
import com.adrianperezcobo.dummycommerce.store.product.domain.Product;
import com.adrianperezcobo.dummycommerce.store.product.domain.ProductImage;
import org.springframework.stereotype.Component;

@Component
public class ProductWebMapper {

    public CreateProductCommand toCommand(CreateProductRequest request) {
        return new CreateProductCommand(
                request.name(),
                request.description(),
                request.price(),
                request.categoryId()
        );
    }

    public ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStatus(),
                product.getCategoryId(),
                product.getImages()
                        .stream()
                        .map(this::toImageResponse)
                        .toList()
        );
    }

    private ProductImageResponse toImageResponse(ProductImage image) {
        return new ProductImageResponse(
                image.getId(),
                image.getObjectKey(),
                image.getAltText(),
                image.getPosition(),
                image.isPrimary()
        );
    }

    public UpdateProductCommand toCommand(UpdateProductRequest request) {
        return new UpdateProductCommand(
                request.name(),
                request.description(),
                request.price(),
                request.categoryId()
        );
    }
}