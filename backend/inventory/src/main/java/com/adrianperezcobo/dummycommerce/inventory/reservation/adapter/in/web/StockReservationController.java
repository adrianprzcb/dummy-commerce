package com.adrianperezcobo.dummycommerce.inventory.reservation.adapter.in.web;

import com.adrianperezcobo.dummycommerce.inventory.reservation.adapter.in.web.request.ReserveStockRequest;
import com.adrianperezcobo.dummycommerce.inventory.reservation.adapter.in.web.response.StockReservationResponse;
import com.adrianperezcobo.dummycommerce.inventory.reservation.application.port.in.ConfirmReservationUseCase;
import com.adrianperezcobo.dummycommerce.inventory.reservation.application.port.in.GetReservationUseCase;
import com.adrianperezcobo.dummycommerce.inventory.reservation.application.port.in.ReleaseReservationUseCase;
import com.adrianperezcobo.dummycommerce.inventory.reservation.application.port.in.ReserveStockUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/reservations")
public class StockReservationController {

    private final ReserveStockUseCase reserveStockUseCase;
    private final GetReservationUseCase getReservationUseCase;
    private final ConfirmReservationUseCase confirmReservationUseCase;
    private final ReleaseReservationUseCase releaseReservationUseCase;
    private final StockReservationWebMapper mapper;

    public StockReservationController(
            ReserveStockUseCase reserveStockUseCase,
            GetReservationUseCase getReservationUseCase,
            ConfirmReservationUseCase confirmReservationUseCase,
            ReleaseReservationUseCase releaseReservationUseCase,
            StockReservationWebMapper mapper
    ) {
        this.reserveStockUseCase =
                reserveStockUseCase;

        this.getReservationUseCase =
                getReservationUseCase;

        this.confirmReservationUseCase =
                confirmReservationUseCase;

        this.releaseReservationUseCase =
                releaseReservationUseCase;

        this.mapper = mapper;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StockReservationResponse reserve(
            @Valid
            @RequestBody
            ReserveStockRequest request
    ) {
        return mapper.toResponse(
                reserveStockUseCase.reserve(
                        mapper.toCommand(request)
                )
        );
    }

    @GetMapping("/{reservationId}")
    public StockReservationResponse get(
            @PathVariable UUID reservationId
    ) {
        return mapper.toResponse(
                getReservationUseCase.getById(
                        reservationId
                )
        );
    }

    @PatchMapping("/{reservationId}/confirm")
    public StockReservationResponse confirm(
            @PathVariable UUID reservationId
    ) {
        return mapper.toResponse(
                confirmReservationUseCase.confirm(
                        reservationId
                )
        );
    }

    @PatchMapping("/{reservationId}/release")
    public StockReservationResponse release(
            @PathVariable UUID reservationId
    ) {
        return mapper.toResponse(
                releaseReservationUseCase.release(
                        reservationId
                )
        );
    }
}