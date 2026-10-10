package com.adrianperezcobo.dummycommerce.store.product.adapter.in.web;

import com.adrianperezcobo.dummycommerce.store.product.application.exception.ProductNotFoundException;
import com.adrianperezcobo.dummycommerce.store.product.application.port.in.ChangeProductStatusUseCase;
import com.adrianperezcobo.dummycommerce.store.product.application.port.in.CreateProductUseCase;
import com.adrianperezcobo.dummycommerce.store.product.application.port.in.GetProductUseCase;
import com.adrianperezcobo.dummycommerce.store.product.application.port.in.UpdateProductUseCase;
import com.adrianperezcobo.dummycommerce.store.product.domain.Product;
import com.adrianperezcobo.dummycommerce.store.product.domain.ProductStatus;
import com.adrianperezcobo.dummycommerce.store.shared.adapter.in.web.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import com.adrianperezcobo.dummycommerce.store.shared.security.*;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        ProductWebMapper.class,
        GlobalExceptionHandler.class
})
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;
    @MockitoBean
    private JwtTokenValidator jwtValidator;

    @BeforeEach
    void authenticateAdmin() {
        org.mockito.Mockito.when(jwtValidator.parse("ADMIN_TOKEN")).thenReturn(
                new AuthenticatedUser(java.util.UUID.randomUUID(), "admin@example.com", "ADMIN"));
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity())
                .defaultRequest(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/")
                        .header("Authorization", "Bearer ADMIN_TOKEN")).build();
    }

    @MockitoBean
    private CreateProductUseCase createProductUseCase;

    @MockitoBean
    private GetProductUseCase getProductUseCase;

    @MockitoBean
    private UpdateProductUseCase updateProductUseCase;

    @MockitoBean
    private ChangeProductStatusUseCase changeProductStatusUseCase;

    @Test
    void shouldCreateProduct() throws Exception {
        UUID categoryId = UUID.randomUUID();

        Product product = new Product(
                UUID.randomUUID(),
                "Keyboard",
                "Mechanical keyboard",
                new BigDecimal("99.99"),
                ProductStatus.ACTIVE,
                categoryId
        );

        when(createProductUseCase.create(any()))
                .thenReturn(product);

        mockMvc.perform(
                        post("/api/products")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "name": "Keyboard",
                                          "description": "Mechanical keyboard",
                                          "price": 99.99,
                                          "categoryId": "%s"
                                        }
                                        """.formatted(categoryId))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id")
                        .value(product.getId().toString()))
                .andExpect(jsonPath("$.name")
                        .value("Keyboard"))
                .andExpect(jsonPath("$.price")
                        .value(99.99))
                .andExpect(jsonPath("$.status")
                        .value("ACTIVE"))
                .andExpect(jsonPath("$.categoryId")
                        .value(categoryId.toString()));
    }

    @Test
    void shouldRejectInvalidProductRequest() throws Exception {
        mockMvc.perform(
                        post("/api/products")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "name": "",
                                          "description": "Invalid product",
                                          "price": -10,
                                          "categoryId": null
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title")
                        .value("Validation error"))
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.price").exists())
                .andExpect(jsonPath("$.errors.categoryId").exists());
    }

    @Test
    void shouldReturnProductById() throws Exception {
        Product product = createProduct();

        when(getProductUseCase.getById(product.getId()))
                .thenReturn(product);

        mockMvc.perform(
                        get("/api/products/{id}", product.getId())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id")
                        .value(product.getId().toString()))
                .andExpect(jsonPath("$.name")
                        .value("Keyboard"));
    }

    @Test
    void shouldReturn404WhenProductDoesNotExist() throws Exception {
        UUID productId = UUID.randomUUID();

        when(getProductUseCase.getById(productId))
                .thenThrow(new ProductNotFoundException(productId));

        mockMvc.perform(
                        get("/api/products/{id}", productId)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title")
                        .value("Product not found"))
                .andExpect(jsonPath("$.detail")
                        .value("Product not found: " + productId));
    }

    @Test
    void shouldDeactivateProduct() throws Exception {
        Product product = createProduct();
        product.deactivate();

        when(changeProductStatusUseCase.deactivate(product.getId()))
                .thenReturn(product);

        mockMvc.perform(
                        patch(
                                "/api/products/{id}/deactivate",
                                product.getId()
                        )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status")
                        .value("INACTIVE"));
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