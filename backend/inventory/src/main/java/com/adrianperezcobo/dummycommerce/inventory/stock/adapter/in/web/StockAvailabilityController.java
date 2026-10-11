package com.adrianperezcobo.dummycommerce.inventory.stock.adapter.in.web;

import com.adrianperezcobo.dummycommerce.inventory.stock.application.port.in.GetStockAvailabilityUseCase;
import com.adrianperezcobo.dummycommerce.inventory.stock.application.result.StockAvailability;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.UUID;

@RestController
public class StockAvailabilityController {
    private final GetStockAvailabilityUseCase availability;

    public StockAvailabilityController(GetStockAvailabilityUseCase availability) {
        this.availability = availability;
    }

    @Operation(summary = "Public stock availability for up to 100 products",
            description = "Returns purchasable units, excluding reservations. Products without inventory return zero.")
    @GetMapping("/api/inventory/availability")
    public List<StockAvailability> getAvailability(@RequestParam List<UUID> productIds) {
        return availability.getAvailability(productIds);
    }
}
