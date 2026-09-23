package com.adrianperezcobo.dummycommerce.store.product.adapter.in.web;

import com.adrianperezcobo.dummycommerce.store.product.application.port.in.*;
import com.adrianperezcobo.dummycommerce.store.product.application.result.PreparedProductImageUpload;
import com.adrianperezcobo.dummycommerce.store.product.domain.Product;
import com.adrianperezcobo.dummycommerce.store.product.domain.ProductStatus;
import com.adrianperezcobo.dummycommerce.store.shared.adapter.in.web.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductImageController.class)
@Import({
        ProductWebMapper.class,
        GlobalExceptionHandler.class
})
class ProductImageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PrepareProductImageUploadUseCase prepareUploadUseCase;

    @MockitoBean
    private ConfirmProductImageUseCase confirmProductImageUseCase;

    @MockitoBean
    private RemoveProductImageUseCase removeProductImageUseCase;

    @MockitoBean
    private SetPrimaryProductImageUseCase setPrimaryProductImageUseCase;

    @MockitoBean
    private GetProductImageContentUseCase getProductImageContentUseCase;

    @Test
    void shouldPrepareUpload() throws Exception {
        UUID productId = UUID.randomUUID();
        UUID imageId = UUID.randomUUID();

        when(prepareUploadUseCase.prepare(productId))
                .thenReturn(
                        new PreparedProductImageUpload(
                                imageId,
                                "http://localhost:9000/presigned"
                        )
                );

        mockMvc.perform(
                        post(
                                "/api/products/{productId}/images/upload-url",
                                productId
                        )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imageId")
                        .value(imageId.toString()))
                .andExpect(jsonPath("$.uploadUrl")
                        .value(
                                "http://localhost:9000/presigned"
                        ));
    }

    @Test
    void shouldConfirmImage() throws Exception {
        Product product = createProduct();

        UUID imageId = UUID.randomUUID();

        when(confirmProductImageUseCase.confirm(
                eq(product.getId()),
                eq(imageId),
                any()
        )).thenReturn(product);

        mockMvc.perform(
                        post(
                                "/api/products/{productId}/images/{imageId}/confirm",
                                product.getId(),
                                imageId
                        )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "altText": "Front view",
                                          "position": 0,
                                          "primary": true
                                        }
                                        """)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id")
                        .value(product.getId().toString()));
    }

    @Test
    void shouldRejectNegativeImagePosition()
            throws Exception {

        UUID productId = UUID.randomUUID();
        UUID imageId = UUID.randomUUID();

        mockMvc.perform(
                        post(
                                "/api/products/{productId}/images/{imageId}/confirm",
                                productId,
                                imageId
                        )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "altText": "Front view",
                                          "position": -1,
                                          "primary": true
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.position").exists());
    }

    @Test
    void shouldRedirectToTemporaryImageUrl()
            throws Exception {

        UUID productId = UUID.randomUUID();
        UUID imageId = UUID.randomUUID();

        when(getProductImageContentUseCase.getAccessUrl(
                productId,
                imageId
        )).thenReturn(
                "http://localhost:9000/product-images/signed"
        );

        mockMvc.perform(
                        get(
                                "/api/products/{productId}/images/{imageId}/content",
                                productId,
                                imageId
                        )
                )
                .andExpect(status().isTemporaryRedirect())
                .andExpect(header().string(
                        "Location",
                        "http://localhost:9000/product-images/signed"
                ));
    }

    @Test
    void shouldDeleteImage() throws Exception {
        UUID productId = UUID.randomUUID();
        UUID imageId = UUID.randomUUID();

        mockMvc.perform(
                        delete(
                                "/api/products/{productId}/images/{imageId}",
                                productId,
                                imageId
                        )
                )
                .andExpect(status().isNoContent());

        verify(removeProductImageUseCase)
                .remove(productId, imageId);
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