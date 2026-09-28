package com.adrianperezcobo.dummycommerce.inventory.reservation.adapter.in.web.response;

import com.adrianperezcobo.dummycommerce.inventory.reservation.domain.ReservationStatus;

import java.time.Instant;
import java.util.UUID;

public record StockReservationResponse(
        UUID id,
        UUID orderId,
        UUID productId,
        int quantity,
        ReservationStatus status,
        Instant createdAt
) {
}