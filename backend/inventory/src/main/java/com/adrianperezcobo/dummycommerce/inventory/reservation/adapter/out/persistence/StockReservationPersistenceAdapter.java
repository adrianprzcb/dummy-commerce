package com.adrianperezcobo.dummycommerce.inventory.reservation.adapter.out.persistence;

import com.adrianperezcobo.dummycommerce.inventory.reservation.application.port.out.StockReservationRepository;
import com.adrianperezcobo.dummycommerce.inventory.reservation.domain.StockReservation;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class StockReservationPersistenceAdapter
        implements StockReservationRepository {

    private final StockReservationJpaRepository repository;

    public StockReservationPersistenceAdapter(
            StockReservationJpaRepository repository
    ) {
        this.repository = repository;
    }

    @Override
    public StockReservation save(
            StockReservation reservation
    ) {
        StockReservationJpaEntity entity =
                new StockReservationJpaEntity(
                        reservation.getId(),
                        reservation.getOrderId(),
                        reservation.getProductId(),
                        reservation.getQuantity(),
                        reservation.getStatus(),
                        reservation.getCreatedAt()
                );

        return toDomain(
                repository.save(entity)
        );
    }

    @Override
    public Optional<StockReservation> findById(
            UUID reservationId
    ) {
        return repository
                .findById(reservationId)
                .map(this::toDomain);
    }

    @Override
    public Optional<StockReservation> findByIdForUpdate(
            UUID reservationId
    ) {
        return repository
                .findByIdForUpdate(reservationId)
                .map(this::toDomain);
    }

    @Override
    public Optional<StockReservation>
    findByOrderIdAndProductId(
            UUID orderId,
            UUID productId
    ) {
        return repository
                .findByOrderIdAndProductId(
                        orderId,
                        productId
                )
                .map(this::toDomain);
    }

    @Override
    public boolean existsByOrderIdAndProductId(
            UUID orderId,
            UUID productId
    ) {
        return repository
                .existsByOrderIdAndProductId(
                        orderId,
                        productId
                );
    }

    private StockReservation toDomain(
            StockReservationJpaEntity entity
    ) {
        return new StockReservation(
                entity.getId(),
                entity.getOrderId(),
                entity.getProductId(),
                entity.getQuantity(),
                entity.getStatus(),
                entity.getCreatedAt()
        );
    }
}