package com.adrianperezcobo.dummycommerce.inventory.stock.application.port.in;

import com.adrianperezcobo.dummycommerce.inventory.stock.application.command.ChangeStockCommand;
import com.adrianperezcobo.dummycommerce.inventory.stock.domain.InventoryItem;

public interface DecreaseStockUseCase {

    InventoryItem decrease(
            ChangeStockCommand command
    );
}