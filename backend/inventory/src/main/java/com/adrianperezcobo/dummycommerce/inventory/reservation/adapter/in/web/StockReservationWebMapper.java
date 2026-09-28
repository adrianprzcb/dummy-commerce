package com.adrianperezcobo.dummycommerce.inventory.reservation.adapter.in.web;

import com.adrianperezcobo.dummycommerce.inventory.reservation.adapter.in.web.request.ReserveStockRequest;
import com.adrianperezcobo.dummycommerce.inventory.reservation.adapter.in.web.response.StockReservationResponse;
import com.adrianperezcobo.dummycommerce.inventory.reservation.application.command.ReserveStockCommand;
import com.adrianperezcobo.dummycommerce.inventory.reservation.domain.StockReservation;
import org.springframework.stereotype.Component;

@Component
public class StockReservationWebMapper {

    public ReserveStockCommand toCommand(
            ReserveStockRequest request
    ) {
        return new ReserveStockCommand(
                request.orderId(),
                request.productId(),
                request.quantity()
        );
    }

    public StockReservationResponse toResponse(
            StockReservation reservation
    ) {
        return new StockReservationResponse(
                reservation.getId(),
                reservation.getOrderId(),
                reservation.getProductId(),
                reservation.getQuantity(),
                reservation.getStatus(),
                reservation.getCreatedAt()
        );
    }
}