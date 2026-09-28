package com.adrianperezcobo.dummycommerce.inventory.stock.adapter.in.web;

import com.adrianperezcobo.dummycommerce.inventory.stock.adapter.in.web.request.ChangeStockRequest;
import com.adrianperezcobo.dummycommerce.inventory.stock.adapter.in.web.request.CreateInventoryItemRequest;
import com.adrianperezcobo.dummycommerce.inventory.stock.adapter.in.web.response.InventoryResponse;
import com.adrianperezcobo.dummycommerce.inventory.stock.application.port.in.CreateInventoryItemUseCase;
import com.adrianperezcobo.dummycommerce.inventory.stock.application.port.in.DecreaseStockUseCase;
import com.adrianperezcobo.dummycommerce.inventory.stock.application.port.in.GetInventoryUseCase;
import com.adrianperezcobo.dummycommerce.inventory.stock.application.port.in.IncreaseStockUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final CreateInventoryItemUseCase createInventoryItemUseCase;
    private final GetInventoryUseCase getInventoryUseCase;
    private final IncreaseStockUseCase increaseStockUseCase;
    private final DecreaseStockUseCase decreaseStockUseCase;
    private final InventoryWebMapper mapper;

    public InventoryController(
            CreateInventoryItemUseCase createInventoryItemUseCase,
            GetInventoryUseCase getInventoryUseCase,
            IncreaseStockUseCase increaseStockUseCase,
            DecreaseStockUseCase decreaseStockUseCase,
            InventoryWebMapper mapper
    ) {
        this.createInventoryItemUseCase =
                createInventoryItemUseCase;

        this.getInventoryUseCase =
                getInventoryUseCase;

        this.increaseStockUseCase =
                increaseStockUseCase;

        this.decreaseStockUseCase =
                decreaseStockUseCase;

        this.mapper = mapper;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryResponse create(
            @Valid
            @RequestBody
            CreateInventoryItemRequest request
    ) {
        return mapper.toResponse(
                createInventoryItemUseCase.create(
                        mapper.toCommand(request)
                )
        );
    }

    @GetMapping("/{productId}")
    public InventoryResponse get(
            @PathVariable UUID productId
    ) {
        return mapper.toResponse(
                getInventoryUseCase.getByProductId(
                        productId
                )
        );
    }

    @PatchMapping("/{productId}/increase")
    public InventoryResponse increase(
            @PathVariable UUID productId,
            @Valid
            @RequestBody
            ChangeStockRequest request
    ) {
        return mapper.toResponse(
                increaseStockUseCase.increase(
                        mapper.toCommand(
                                productId,
                                request
                        )
                )
        );
    }

    @PatchMapping("/{productId}/decrease")
    public InventoryResponse decrease(
            @PathVariable UUID productId,
            @Valid
            @RequestBody
            ChangeStockRequest request
    ) {
        return mapper.toResponse(
                decreaseStockUseCase.decrease(
                        mapper.toCommand(
                                productId,
                                request
                        )
                )
        );
    }
}