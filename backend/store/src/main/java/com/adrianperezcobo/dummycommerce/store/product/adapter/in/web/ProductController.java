package com.adrianperezcobo.dummycommerce.store.product.adapter.in.web;

import com.adrianperezcobo.dummycommerce.store.product.adapter.in.web.request.CreateProductRequest;
import com.adrianperezcobo.dummycommerce.store.product.adapter.in.web.request.UpdateProductRequest;
import com.adrianperezcobo.dummycommerce.store.product.adapter.in.web.response.ProductResponse;
import com.adrianperezcobo.dummycommerce.store.product.application.port.in.ChangeProductStatusUseCase;
import com.adrianperezcobo.dummycommerce.store.product.application.port.in.CreateProductUseCase;
import com.adrianperezcobo.dummycommerce.store.product.application.port.in.GetProductUseCase;
import com.adrianperezcobo.dummycommerce.store.product.application.port.in.UpdateProductUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final CreateProductUseCase createProductUseCase;
    private final GetProductUseCase getProductUseCase;
    private final ProductWebMapper mapper;
    private final UpdateProductUseCase updateProductUseCase;
    private final ChangeProductStatusUseCase changeProductStatusUseCase;

    public ProductController(
            CreateProductUseCase createProductUseCase,
            GetProductUseCase getProductUseCase,
            ProductWebMapper mapper,
            UpdateProductUseCase updateProductUseCase,
            ChangeProductStatusUseCase changeProductStatusUseCase
    ) {
        this.createProductUseCase = createProductUseCase;
        this.getProductUseCase = getProductUseCase;
        this.mapper = mapper;
        this.updateProductUseCase = updateProductUseCase;
        this.changeProductStatusUseCase = changeProductStatusUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse create(
            @Valid @RequestBody CreateProductRequest request
    ) {
        return mapper.toResponse(
                createProductUseCase.create(
                        mapper.toCommand(request)
                )
        );
    }

    @PutMapping("/{id}")
    public ProductResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProductRequest request
    ) {
        return mapper.toResponse(
                updateProductUseCase.update(
                        id,
                        mapper.toCommand(request)
                )
        );
    }

    @PatchMapping("/{id}/activate")
    public ProductResponse activate(@PathVariable UUID id){
        return mapper.toResponse(
                changeProductStatusUseCase.activate(id)
        );
    }

    @PatchMapping("/{id}/deactivate")
    public ProductResponse deactivate(@PathVariable UUID id){
        return mapper.toResponse(
                changeProductStatusUseCase.deactivate(id)
        );
    }

    @PatchMapping("/{id}/discontinue")
    public ProductResponse discontinue(@PathVariable UUID id){
        return mapper.toResponse(
                changeProductStatusUseCase.discontinue(id)
        );
    }



    @GetMapping("/{id}")
    public ProductResponse getById(@PathVariable UUID id) {
        return mapper.toResponse(
                getProductUseCase.getById(id)
        );
    }

    @GetMapping
    public List<ProductResponse> getAll() {
        return getProductUseCase.getAll()
                .stream()
                .map(mapper::toResponse)
                .toList();
    }
}