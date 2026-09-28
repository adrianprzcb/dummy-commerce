package com.adrianperezcobo.dummycommerce.inventory.reservation.application.service;

import com.adrianperezcobo.dummycommerce.inventory.reservation.application.command.ReserveStockCommand;
import com.adrianperezcobo.dummycommerce.inventory.reservation.application.exception.ReservationAlreadyExistsException;
import com.adrianperezcobo.dummycommerce.inventory.reservation.application.exception.ReservationNotFoundException;
import com.adrianperezcobo.dummycommerce.inventory.reservation.application.port.out.StockReservationRepository;
import com.adrianperezcobo.dummycommerce.inventory.reservation.domain.ReservationStatus;
import com.adrianperezcobo.dummycommerce.inventory.reservation.domain.StockReservation;
import com.adrianperezcobo.dummycommerce.inventory.stock.application.port.out.InventoryRepository;
import com.adrianperezcobo.dummycommerce.inventory.stock.domain.InventoryItem;
import com.adrianperezcobo.dummycommerce.inventory.stock.domain.exception.InsufficientStockException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockReservationServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private StockReservationRepository reservationRepository;

    @InjectMocks
    private StockReservationService service;

    @Test
    void shouldReserveStock() {
        UUID orderId = UUID.randomUUID();

        InventoryItem item =
                new InventoryItem(
                        UUID.randomUUID(),
                        10,
                        0
                );

        when(inventoryRepository
                .findByProductIdForUpdate(
                        item.getProductId()
                ))
                .thenReturn(Optional.of(item));

        when(reservationRepository
                .existsByOrderIdAndProductId(
                        orderId,
                        item.getProductId()
                ))
                .thenReturn(false);

        when(inventoryRepository.save(any()))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        when(reservationRepository.save(any()))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        StockReservation result =
                service.reserve(
                        new ReserveStockCommand(
                                orderId,
                                item.getProductId(),
                                3
                        )
                );

        assertThat(item.getAvailableQuantity())
                .isEqualTo(7);

        assertThat(item.getReservedQuantity())
                .isEqualTo(3);

        assertThat(result.getOrderId())
                .isEqualTo(orderId);

        assertThat(result.getQuantity())
                .isEqualTo(3);

        assertThat(result.getStatus())
                .isEqualTo(
                        ReservationStatus.RESERVED
                );

        verify(inventoryRepository)
                .findByProductIdForUpdate(
                        item.getProductId()
                );

        verify(inventoryRepository).save(item);

        verify(reservationRepository)
                .save(any(StockReservation.class));
    }

    @Test
    void shouldRejectDuplicatedReservation() {
        UUID orderId = UUID.randomUUID();

        InventoryItem item =
                new InventoryItem(
                        UUID.randomUUID(),
                        10,
                        0
                );

        when(inventoryRepository
                .findByProductIdForUpdate(
                        item.getProductId()
                ))
                .thenReturn(Optional.of(item));

        when(reservationRepository
                .existsByOrderIdAndProductId(
                        orderId,
                        item.getProductId()
                ))
                .thenReturn(true);

        assertThatThrownBy(() ->
                service.reserve(
                        new ReserveStockCommand(
                                orderId,
                                item.getProductId(),
                                3
                        )
                )
        ).isInstanceOf(
                ReservationAlreadyExistsException.class
        );

        assertThat(item.getAvailableQuantity())
                .isEqualTo(10);

        assertThat(item.getReservedQuantity())
                .isZero();

        verify(inventoryRepository, never())
                .save(any());

        verify(reservationRepository, never())
                .save(any());
    }

    @Test
    void shouldRejectReservationWithInsufficientStock() {
        UUID orderId = UUID.randomUUID();

        InventoryItem item =
                new InventoryItem(
                        UUID.randomUUID(),
                        2,
                        0
                );

        when(inventoryRepository
                .findByProductIdForUpdate(
                        item.getProductId()
                ))
                .thenReturn(Optional.of(item));

        when(reservationRepository
                .existsByOrderIdAndProductId(
                        orderId,
                        item.getProductId()
                ))
                .thenReturn(false);

        assertThatThrownBy(() ->
                service.reserve(
                        new ReserveStockCommand(
                                orderId,
                                item.getProductId(),
                                3
                        )
                )
        ).isInstanceOf(
                InsufficientStockException.class
        );

        verify(inventoryRepository, never())
                .save(any());

        verify(reservationRepository, never())
                .save(any());
    }

    @Test
    void shouldConfirmReservation() {
        StockReservation reservation =
                createReservation(
                        ReservationStatus.RESERVED,
                        3
                );

        InventoryItem item =
                new InventoryItem(
                        reservation.getProductId(),
                        7,
                        3
                );

        when(reservationRepository
                .findByIdForUpdate(
                        reservation.getId()
                ))
                .thenReturn(
                        Optional.of(reservation)
                );

        when(inventoryRepository
                .findByProductIdForUpdate(
                        reservation.getProductId()
                ))
                .thenReturn(Optional.of(item));

        when(inventoryRepository.save(item))
                .thenReturn(item);

        when(reservationRepository
                .save(reservation))
                .thenReturn(reservation);

        StockReservation result =
                service.confirm(
                        reservation.getId()
                );

        assertThat(result.getStatus())
                .isEqualTo(
                        ReservationStatus.CONFIRMED
                );

        assertThat(item.getAvailableQuantity())
                .isEqualTo(7);

        assertThat(item.getReservedQuantity())
                .isZero();

        assertThat(item.getTotalQuantity())
                .isEqualTo(7);
    }

    @Test
    void shouldReleaseReservation() {
        StockReservation reservation =
                createReservation(
                        ReservationStatus.RESERVED,
                        3
                );

        InventoryItem item =
                new InventoryItem(
                        reservation.getProductId(),
                        7,
                        3
                );

        when(reservationRepository
                .findByIdForUpdate(
                        reservation.getId()
                ))
                .thenReturn(
                        Optional.of(reservation)
                );

        when(inventoryRepository
                .findByProductIdForUpdate(
                        reservation.getProductId()
                ))
                .thenReturn(Optional.of(item));

        when(inventoryRepository.save(item))
                .thenReturn(item);

        when(reservationRepository
                .save(reservation))
                .thenReturn(reservation);

        StockReservation result =
                service.release(
                        reservation.getId()
                );

        assertThat(result.getStatus())
                .isEqualTo(
                        ReservationStatus.RELEASED
                );

        assertThat(item.getAvailableQuantity())
                .isEqualTo(10);

        assertThat(item.getReservedQuantity())
                .isZero();

        assertThat(item.getTotalQuantity())
                .isEqualTo(10);
    }

    @Test
    void shouldThrowWhenReservationDoesNotExist() {
        UUID reservationId = UUID.randomUUID();

        when(reservationRepository
                .findByIdForUpdate(
                        reservationId
                ))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.confirm(reservationId)
        ).isInstanceOf(
                ReservationNotFoundException.class
        );

        verifyNoInteractions(inventoryRepository);
    }

    private StockReservation createReservation(
            ReservationStatus status,
            int quantity
    ) {
        return new StockReservation(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                quantity,
                status,
                Instant.now()
        );
    }
}