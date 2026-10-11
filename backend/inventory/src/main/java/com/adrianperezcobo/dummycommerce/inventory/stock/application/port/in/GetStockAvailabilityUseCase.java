package com.adrianperezcobo.dummycommerce.inventory.stock.application.port.in;

import com.adrianperezcobo.dummycommerce.inventory.stock.application.result.StockAvailability;
import java.util.List;
import java.util.UUID;

public interface GetStockAvailabilityUseCase {
    List<StockAvailability> getAvailability(List<UUID> productIds);
}
