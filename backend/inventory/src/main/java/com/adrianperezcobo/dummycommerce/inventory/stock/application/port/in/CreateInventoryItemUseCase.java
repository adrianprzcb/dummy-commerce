package com.adrianperezcobo.dummycommerce.inventory.stock.application.port.in;

import com.adrianperezcobo.dummycommerce.inventory.stock.application.command.CreateInventoryItemCommand;
import com.adrianperezcobo.dummycommerce.inventory.stock.domain.InventoryItem;

public interface CreateInventoryItemUseCase {

    InventoryItem create(
            CreateInventoryItemCommand command
    );
}