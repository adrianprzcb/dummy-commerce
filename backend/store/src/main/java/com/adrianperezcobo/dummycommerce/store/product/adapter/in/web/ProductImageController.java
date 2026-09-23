package com.adrianperezcobo.dummycommerce.store.product.adapter.in.web;

import com.adrianperezcobo.dummycommerce.store.product.adapter.in.web.request.ConfirmProductImageRequest;
import com.adrianperezcobo.dummycommerce.store.product.adapter.in.web.response.PrepareProductImageUploadResponse;
import com.adrianperezcobo.dummycommerce.store.product.adapter.in.web.response.ProductResponse;
import com.adrianperezcobo.dummycommerce.store.product.application.command.ConfirmProductImageCommand;
import com.adrianperezcobo.dummycommerce.store.product.application.port.in.ConfirmProductImageUseCase;
import com.adrianperezcobo.dummycommerce.store.product.application.port.in.PrepareProductImageUploadUseCase;
import com.adrianperezcobo.dummycommerce.store.product.application.result.PreparedProductImageUpload;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/products/{productId}/images")
public class ProductImageController {

    private final PrepareProductImageUploadUseCase prepareUploadUseCase;
    private final ConfirmProductImageUseCase confirmProductImageUseCase;
    private final ProductWebMapper productWebMapper;

    public ProductImageController(
            PrepareProductImageUploadUseCase prepareUploadUseCase,
            ConfirmProductImageUseCase confirmProductImageUseCase,
            ProductWebMapper productWebMapper

    ) {
        this.prepareUploadUseCase = prepareUploadUseCase;
        this.confirmProductImageUseCase = confirmProductImageUseCase;
        this.productWebMapper = productWebMapper;
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

    @PostMapping("/{imageId}/confirm")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse confirm(
            @PathVariable UUID productId,
            @PathVariable UUID imageId,
            @Valid @RequestBody ConfirmProductImageRequest request
    ) {
        return productWebMapper.toResponse(
                confirmProductImageUseCase.confirm(
                        productId,
                        imageId,
                        new ConfirmProductImageCommand(
                                request.altText(),
                                request.position(),
                                request.primary()
                        )
                )
        );
    }

}