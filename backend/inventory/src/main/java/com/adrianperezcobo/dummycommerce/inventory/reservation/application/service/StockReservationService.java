package com.adrianperezcobo.dummycommerce.inventory.reservation.application.service;

import com.adrianperezcobo.dummycommerce.inventory.reservation.application.command.ReserveStockCommand;
import com.adrianperezcobo.dummycommerce.inventory.reservation.application.exception.ReservationAlreadyExistsException;
import com.adrianperezcobo.dummycommerce.inventory.reservation.application.exception.ReservationNotFoundException;
import com.adrianperezcobo.dummycommerce.inventory.reservation.application.port.in.ConfirmReservationUseCase;
import com.adrianperezcobo.dummycommerce.inventory.reservation.application.port.in.GetReservationUseCase;
import com.adrianperezcobo.dummycommerce.inventory.reservation.application.port.in.ReleaseReservationUseCase;
import com.adrianperezcobo.dummycommerce.inventory.reservation.application.port.in.ReserveStockUseCase;
import com.adrianperezcobo.dummycommerce.inventory.reservation.application.port.out.StockReservationRepository;
import com.adrianperezcobo.dummycommerce.inventory.reservation.domain.ReservationStatus;
import com.adrianperezcobo.dummycommerce.inventory.reservation.domain.StockReservation;
import com.adrianperezcobo.dummycommerce.inventory.stock.application.exception.InventoryItemNotFoundException;
import com.adrianperezcobo.dummycommerce.inventory.stock.application.port.out.InventoryRepository;
import com.adrianperezcobo.dummycommerce.inventory.stock.domain.InventoryItem;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class StockReservationService implements
        ReserveStockUseCase,
        ConfirmReservationUseCase,
        ReleaseReservationUseCase,
        GetReservationUseCase {

    private final InventoryRepository inventoryRepository;
    private final StockReservationRepository reservationRepository;

    public StockReservationService(
            InventoryRepository inventoryRepository,
            StockReservationRepository reservationRepository
    ) {
        this.inventoryRepository = inventoryRepository;
        this.reservationRepository = reservationRepository;
    }

    @Override
    @Transactional
    public StockReservation reserve(
            ReserveStockCommand command
    ) {
        InventoryItem item = inventoryRepository
                .findByProductIdForUpdate(
                        command.productId()
                )
                .orElseThrow(
                        () -> new InventoryItemNotFoundException(
                                command.productId()
                        )
                );

        if (reservationRepository
                .existsByOrderIdAndProductId(
                        command.orderId(),
                        command.productId()
                )) {
            throw new ReservationAlreadyExistsException(
                    command.orderId(),
                    command.productId()
            );
        }

        item.reserve(command.quantity());

        StockReservation reservation =
                new StockReservation(
                        UUID.randomUUID(),
                        command.orderId(),
                        command.productId(),
                        command.quantity(),
                        ReservationStatus.RESERVED,
                        Instant.now()
                );

        inventoryRepository.save(item);

        return reservationRepository.save(
                reservation
        );
    }

    @Override
    @Transactional
    public StockReservation confirm(
            UUID reservationId
    ) {
        StockReservation reservation =
                reservationRepository
                        .findByIdForUpdate(
                                reservationId
                        )
                        .orElseThrow(
                                () -> new ReservationNotFoundException(
                                        reservationId
                                )
                        );

        InventoryItem item = inventoryRepository
                .findByProductIdForUpdate(
                        reservation.getProductId()
                )
                .orElseThrow(
                        () -> new InventoryItemNotFoundException(
                                reservation.getProductId()
                        )
                );

        item.confirmReservation(
                reservation.getQuantity()
        );

        reservation.confirm();

        inventoryRepository.save(item);

        return reservationRepository.save(
                reservation
        );
    }

    @Override
    @Transactional
    public StockReservation release(
            UUID reservationId
    ) {
        StockReservation reservation =
                reservationRepository
                        .findByIdForUpdate(
                                reservationId
                        )
                        .orElseThrow(
                                () -> new ReservationNotFoundException(
                                        reservationId
                                )
                        );

        InventoryItem item = inventoryRepository
                .findByProductIdForUpdate(
                        reservation.getProductId()
                )
                .orElseThrow(
                        () -> new InventoryItemNotFoundException(
                                reservation.getProductId()
                        )
                );

        item.release(
                reservation.getQuantity()
        );

        reservation.release();

        inventoryRepository.save(item);

        return reservationRepository.save(
                reservation
        );
    }

    @Override
    @Transactional(readOnly = true)
    public StockReservation getById(
            UUID reservationId
    ) {
        return reservationRepository
                .findById(reservationId)
                .orElseThrow(
                        () -> new ReservationNotFoundException(
                                reservationId
                        )
                );
    }
}