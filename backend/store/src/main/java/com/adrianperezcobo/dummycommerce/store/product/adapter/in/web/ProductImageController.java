package com.adrianperezcobo.dummycommerce.store.product.adapter.in.web;

import com.adrianperezcobo.dummycommerce.store.product.adapter.in.web.response.PrepareProductImageUploadResponse;
import com.adrianperezcobo.dummycommerce.store.product.application.port.in.PrepareProductImageUploadUseCase;
import com.adrianperezcobo.dummycommerce.store.product.application.result.PreparedProductImageUpload;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/products/{productId}/images")
public class ProductImageController {

    private final PrepareProductImageUploadUseCase prepareUploadUseCase;

    public ProductImageController(
            PrepareProductImageUploadUseCase prepareUploadUseCase
    ) {
        this.prepareUploadUseCase = prepareUploadUseCase;
    }

    @PostMapping("/upload-url")
    public PrepareProductImageUploadResponse prepareUpload(
            @PathVariable UUID productId
    ) {
        PreparedProductImageUpload upload =
                prepareUploadUseCase.prepare(productId);

        return new PrepareProductImageUploadResponse(
                upload.imageId(),
                upload.uploadUrl()
        );
    }
}