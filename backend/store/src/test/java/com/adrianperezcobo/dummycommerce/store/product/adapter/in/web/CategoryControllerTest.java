package com.adrianperezcobo.dummycommerce.store.category.adapter.in.web;

import com.adrianperezcobo.dummycommerce.store.category.application.exception.CategoryNotFoundException;
import com.adrianperezcobo.dummycommerce.store.category.application.port.in.CreateCategoryUseCase;
import com.adrianperezcobo.dummycommerce.store.category.application.port.in.GetCategoryUseCase;
import com.adrianperezcobo.dummycommerce.store.category.domain.Category;
import com.adrianperezcobo.dummycommerce.store.shared.adapter.in.web.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CategoryController.class)
@Import({
        CategoryWebMapper.class,
        GlobalExceptionHandler.class
})
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateCategoryUseCase createCategoryUseCase;

    @MockitoBean
    private GetCategoryUseCase getCategoryUseCase;

    @Test
    void shouldCreateCategory() throws Exception {
        UUID id = UUID.randomUUID();

        Category category = new Category(
                id,
                "Electronics"
        );

        when(createCategoryUseCase.create(any()))
                .thenReturn(category);

        mockMvc.perform(
                        post("/api/categories")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "name": "Electronics"
                                        }
                                        """)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id")
                        .value(id.toString()))
                .andExpect(jsonPath("$.name")
                        .value("Electronics"));
    }

    @Test
    void shouldRejectBlankCategoryName() throws Exception {
        mockMvc.perform(
                        post("/api/categories")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "name": ""
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title")
                        .value("Validation error"))
                .andExpect(jsonPath("$.errors.name").exists());
    }

    @Test
    void shouldReturnAllCategories() throws Exception {
        when(getCategoryUseCase.getAll())
                .thenReturn(List.of(
                        new Category(
                                UUID.randomUUID(),
                                "Electronics"
                        ),
                        new Category(
                                UUID.randomUUID(),
                                "Gaming"
                        )
                ));

        mockMvc.perform(
                        get("/api/categories")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()")
                        .value(2))
                .andExpect(jsonPath("$[0].name")
                        .value("Electronics"))
                .andExpect(jsonPath("$[1].name")
                        .value("Gaming"));
    }

    @Test
    void shouldReturn404WhenCategoryDoesNotExist()
            throws Exception {

        UUID id = UUID.randomUUID();

        when(getCategoryUseCase.getById(id))
                .thenThrow(new CategoryNotFoundException(id));

        mockMvc.perform(
                        get("/api/categories/{id}", id)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title")
                        .value("Category not found"));
    }
}