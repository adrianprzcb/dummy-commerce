package com.adrianperezcobo.dummycommerce.inventory.stock.adapter.out.persistence;

import com.adrianperezcobo.dummycommerce.inventory.stock.application.port.out.InventoryRepository;
import com.adrianperezcobo.dummycommerce.inventory.stock.domain.InventoryItem;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class InventoryPersistenceAdapter
        implements InventoryRepository {

    private final InventoryItemJpaRepository repository;

    public InventoryPersistenceAdapter(
            InventoryItemJpaRepository repository
    ) {
        this.repository = repository;
    }

    @Override
    public InventoryItem save(InventoryItem item) {
        InventoryItemJpaEntity entity =
                new InventoryItemJpaEntity(
                        item.getProductId(),
                        item.getAvailableQuantity(),
                        item.getReservedQuantity()
                );

        return toDomain(
                repository.save(entity)
        );
    }

    @Override
    public Optional<InventoryItem> findByProductId(
            UUID productId
    ) {
        return repository.findById(productId)
                .map(this::toDomain);
    }

    @Override
    public Optional<InventoryItem> findByProductIdForUpdate(
            UUID productId
    ) {
        return repository
                .findByProductIdForUpdate(productId)
                .map(this::toDomain);
    }

    @Override
    public boolean existsByProductId(UUID productId) {
        return repository.existsById(productId);
    }

    private InventoryItem toDomain(
            InventoryItemJpaEntity entity
    ) {
        return new InventoryItem(
                entity.getProductId(),
                entity.getAvailableQuantity(),
                entity.getReservedQuantity()
        );
    }
}