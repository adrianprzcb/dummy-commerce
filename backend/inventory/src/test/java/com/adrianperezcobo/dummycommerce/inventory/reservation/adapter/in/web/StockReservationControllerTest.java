package com.adrianperezcobo.dummycommerce.inventory.reservation.adapter.in.web;

import com.adrianperezcobo.dummycommerce.inventory.reservation.application.command.ReserveStockCommand;
import com.adrianperezcobo.dummycommerce.inventory.reservation.application.exception.ReservationAlreadyExistsException;
import com.adrianperezcobo.dummycommerce.inventory.reservation.application.exception.ReservationNotFoundException;
import com.adrianperezcobo.dummycommerce.inventory.reservation.application.port.in.ConfirmReservationUseCase;
import com.adrianperezcobo.dummycommerce.inventory.reservation.application.port.in.GetReservationUseCase;
import com.adrianperezcobo.dummycommerce.inventory.reservation.application.port.in.ReleaseReservationUseCase;
import com.adrianperezcobo.dummycommerce.inventory.reservation.application.port.in.ReserveStockUseCase;
import com.adrianperezcobo.dummycommerce.inventory.reservation.domain.ReservationStatus;
import com.adrianperezcobo.dummycommerce.inventory.reservation.domain.StockReservation;
import com.adrianperezcobo.dummycommerce.inventory.reservation.domain.exception.InvalidReservationStateException;
import com.adrianperezcobo.dummycommerce.inventory.shared.adapter.in.web.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StockReservationController.class)
@Import({
        StockReservationWebMapper.class,
        GlobalExceptionHandler.class
})
class StockReservationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReserveStockUseCase reserveStockUseCase;

    @MockitoBean
    private GetReservationUseCase getReservationUseCase;

    @MockitoBean
    private ConfirmReservationUseCase confirmReservationUseCase;

    @MockitoBean
    private ReleaseReservationUseCase releaseReservationUseCase;

    @Test
    void shouldReserveStock() throws Exception {
        UUID reservationId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        StockReservation reservation =
                new StockReservation(
                        reservationId,
                        orderId,
                        productId,
                        3,
                        ReservationStatus.RESERVED,
                        Instant.now()
                );

        when(reserveStockUseCase.reserve(
                any(ReserveStockCommand.class)
        )).thenReturn(reservation);

        mockMvc.perform(
                        post("/api/reservations")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "orderId": "%s",
                                          "productId": "%s",
                                          "quantity": 3
                                        }
                                        """.formatted(
                                        orderId,
                                        productId
                                ))
                )
                .andExpect(status().isCreated())
                .andExpect(
                        jsonPath("$.id")
                                .value(
                                        reservationId.toString()
                                )
                )
                .andExpect(
                        jsonPath("$.orderId")
                                .value(orderId.toString())
                )
                .andExpect(
                        jsonPath("$.productId")
                                .value(productId.toString())
                )
                .andExpect(
                        jsonPath("$.quantity")
                                .value(3)
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("RESERVED")
                )
                .andExpect(
                        jsonPath("$.createdAt")
                                .exists()
                );
    }

    @Test
    void shouldRejectInvalidReservationQuantity()
            throws Exception {

        mockMvc.perform(
                        post("/api/reservations")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "orderId": "%s",
                                          "productId": "%s",
                                          "quantity": 0
                                        }
                                        """.formatted(
                                        UUID.randomUUID(),
                                        UUID.randomUUID()
                                ))
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.title")
                                .value("Validation error")
                )
                .andExpect(
                        jsonPath("$.errors.quantity")
                                .exists()
                );
    }

    @Test
    void shouldReturnConflictForDuplicatedReservation()
            throws Exception {

        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        when(reserveStockUseCase.reserve(
                any(ReserveStockCommand.class)
        )).thenThrow(
                new ReservationAlreadyExistsException(
                        orderId,
                        productId
                )
        );

        mockMvc.perform(
                        post("/api/reservations")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "orderId": "%s",
                                          "productId": "%s",
                                          "quantity": 2
                                        }
                                        """.formatted(
                                        orderId,
                                        productId
                                ))
                )
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.title")
                                .value(
                                        "Reservation already exists"
                                )
                )
                .andExpect(
                        jsonPath("$.status")
                                .value(409)
                );
    }

    @Test
    void shouldGetReservation() throws Exception {
        StockReservation reservation =
                createReservation(
                        ReservationStatus.RESERVED
                );

        when(getReservationUseCase.getById(
                reservation.getId()
        )).thenReturn(reservation);

        mockMvc.perform(
                        get(
                                "/api/reservations/{reservationId}",
                                reservation.getId()
                        )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.id")
                                .value(
                                        reservation
                                                .getId()
                                                .toString()
                                )
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("RESERVED")
                );
    }

    @Test
    void shouldReturnNotFoundWhenReservationDoesNotExist()
            throws Exception {

        UUID reservationId = UUID.randomUUID();

        when(getReservationUseCase
                .getById(reservationId))
                .thenThrow(
                        new ReservationNotFoundException(
                                reservationId
                        )
                );

        mockMvc.perform(
                        get(
                                "/api/reservations/{reservationId}",
                                reservationId
                        )
                )
                .andExpect(status().isNotFound())
                .andExpect(
                        jsonPath("$.title")
                                .value(
                                        "Reservation not found"
                                )
                )
                .andExpect(
                        jsonPath("$.status")
                                .value(404)
                );
    }

    @Test
    void shouldConfirmReservation() throws Exception {
        StockReservation reservation =
                createReservation(
                        ReservationStatus.RESERVED
                );

        reservation.confirm();

        when(confirmReservationUseCase.confirm(
                reservation.getId()
        )).thenReturn(reservation);

        mockMvc.perform(
                        patch(
                                "/api/reservations/{reservationId}/confirm",
                                reservation.getId()
                        )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.status")
                                .value("CONFIRMED")
                );
    }

    @Test
    void shouldReleaseReservation() throws Exception {
        StockReservation reservation =
                createReservation(
                        ReservationStatus.RESERVED
                );

        reservation.release();

        when(releaseReservationUseCase.release(
                reservation.getId()
        )).thenReturn(reservation);

        mockMvc.perform(
                        patch(
                                "/api/reservations/{reservationId}/release",
                                reservation.getId()
                        )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.status")
                                .value("RELEASED")
                );
    }

    @Test
    void shouldReturnConflictForInvalidReservationState()
            throws Exception {

        UUID reservationId = UUID.randomUUID();

        when(confirmReservationUseCase.confirm(
                reservationId
        )).thenThrow(
                new InvalidReservationStateException(
                        "Reservation must be RESERVED"
                )
        );

        mockMvc.perform(
                        patch(
                                "/api/reservations/{reservationId}/confirm",
                                reservationId
                        )
                )
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.title")
                                .value("Invalid state")
                )
                .andExpect(
                        jsonPath("$.status")
                                .value(409)
                );
    }

    private StockReservation createReservation(
            ReservationStatus status
    ) {
        return new StockReservation(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                3,
                status,
                Instant.now()
        );
    }
}