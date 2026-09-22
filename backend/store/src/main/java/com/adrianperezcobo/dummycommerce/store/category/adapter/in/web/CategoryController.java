package com.adrianperezcobo.dummycommerce.store.category.adapter.in.web;

import com.adrianperezcobo.dummycommerce.store.category.adapter.in.web.request.CreateCategoryRequest;
import com.adrianperezcobo.dummycommerce.store.category.adapter.in.web.response.CategoryResponse;
import com.adrianperezcobo.dummycommerce.store.category.application.port.in.CreateCategoryUseCase;
import com.adrianperezcobo.dummycommerce.store.category.application.port.in.GetCategoryUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CreateCategoryUseCase createCategoryUseCase;
    private final GetCategoryUseCase getCategoryUseCase;
    private final CategoryWebMapper mapper;

    public CategoryController(
            CreateCategoryUseCase createCategoryUseCase,
            GetCategoryUseCase getCategoryUseCase,
            CategoryWebMapper mapper
    ) {
        this.createCategoryUseCase = createCategoryUseCase;
        this.getCategoryUseCase = getCategoryUseCase;
        this.mapper = mapper;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse create(
            @Valid @RequestBody CreateCategoryRequest request
    ) {
        return mapper.toResponse(
                createCategoryUseCase.create(
                        mapper.toCommand(request)
                )
        );
    }

    @GetMapping("/{id}")
    public CategoryResponse getById(@PathVariable UUID id) {
        return mapper.toResponse(
                getCategoryUseCase.getById(id)
        );
    }

    @GetMapping
    public List<CategoryResponse> getAll() {
        return getCategoryUseCase.getAll()
                .stream()
                .map(mapper::toResponse)
                .toList();
    }
}