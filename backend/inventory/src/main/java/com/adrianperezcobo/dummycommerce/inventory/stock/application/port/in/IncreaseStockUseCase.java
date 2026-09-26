package com.adrianperezcobo.dummycommerce.inventory.stock.application.port.in;

import com.adrianperezcobo.dummycommerce.inventory.stock.application.command.ChangeStockCommand;
import com.adrianperezcobo.dummycommerce.inventory.stock.domain.InventoryItem;

public interface IncreaseStockUseCase {

    InventoryItem increase(
            ChangeStockCommand command
    );
}