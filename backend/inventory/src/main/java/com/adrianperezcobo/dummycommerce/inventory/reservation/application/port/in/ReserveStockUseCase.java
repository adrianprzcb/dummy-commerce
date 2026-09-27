package com.adrianperezcobo.dummycommerce.inventory.reservation.application.port.in;

import com.adrianperezcobo.dummycommerce.inventory.reservation.application.command.ReserveStockCommand;
import com.adrianperezcobo.dummycommerce.inventory.reservation.domain.StockReservation;

public interface ReserveStockUseCase {

    StockReservation reserve(
            ReserveStockCommand command
    );
}