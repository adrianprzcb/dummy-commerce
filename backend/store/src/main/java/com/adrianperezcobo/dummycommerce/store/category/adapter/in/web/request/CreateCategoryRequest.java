package com.adrianperezcobo.dummycommerce.store.category.adapter.in.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCategoryRequest(

        @NotBlank
        @Size(max = 100)
        String name

) {
}