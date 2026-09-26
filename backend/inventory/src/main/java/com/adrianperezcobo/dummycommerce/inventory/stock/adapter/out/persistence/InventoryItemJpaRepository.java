package com.adrianperezcobo.dummycommerce.inventory.stock.adapter.out.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface InventoryItemJpaRepository
        extends JpaRepository<InventoryItemJpaEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT i
            FROM InventoryItemJpaEntity i
            WHERE i.productId = :productId
            """)
    Optional<InventoryItemJpaEntity> findByProductIdForUpdate(
            @Param("productId") UUID productId
    );
}