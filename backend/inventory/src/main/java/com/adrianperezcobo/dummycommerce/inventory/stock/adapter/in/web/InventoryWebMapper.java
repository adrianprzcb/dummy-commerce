package com.adrianperezcobo.dummycommerce.inventory.stock.adapter.in.web;

import com.adrianperezcobo.dummycommerce.inventory.stock.adapter.in.web.request.ChangeStockRequest;
import com.adrianperezcobo.dummycommerce.inventory.stock.adapter.in.web.request.CreateInventoryItemRequest;
import com.adrianperezcobo.dummycommerce.inventory.stock.adapter.in.web.response.InventoryResponse;
import com.adrianperezcobo.dummycommerce.inventory.stock.application.command.ChangeStockCommand;
import com.adrianperezcobo.dummycommerce.inventory.stock.application.command.CreateInventoryItemCommand;
import com.adrianperezcobo.dummycommerce.inventory.stock.domain.InventoryItem;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class InventoryWebMapper {

    public CreateInventoryItemCommand toCommand(
            CreateInventoryItemRequest request
    ) {
        return new CreateInventoryItemCommand(
                request.productId(),
                request.initialQuantity()
        );
    }

    public ChangeStockCommand toCommand(
            UUID productId,
            ChangeStockRequest request
    ) {
        return new ChangeStockCommand(
                productId,
                request.quantity()
        );
    }

    public InventoryResponse toResponse(
            InventoryItem item
    ) {
        return new InventoryResponse(
                item.getProductId(),
                item.getAvailableQuantity(),
                item.getReservedQuantity(),
                item.getTotalQuantity()
        );
    }
}