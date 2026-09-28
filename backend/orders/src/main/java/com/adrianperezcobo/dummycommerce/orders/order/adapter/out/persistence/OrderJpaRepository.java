package com.adrianperezcobo.dummycommerce.orders.order.adapter.out.persistence;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderJpaRepository
        extends JpaRepository<OrderJpaEntity, UUID> {

    @EntityGraph(attributePaths = "items")
    @Query("""
            SELECT o
            FROM OrderJpaEntity o
            WHERE o.id = :orderId
            """)
    Optional<OrderJpaEntity> findByIdWithItems(
            @Param("orderId") UUID orderId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT o
            FROM OrderJpaEntity o
            WHERE o.id = :orderId
            """)
    Optional<OrderJpaEntity> findByIdForUpdate(
            @Param("orderId") UUID orderId
    );

    @EntityGraph(attributePaths = "items")
    List<OrderJpaEntity>
    findByUserIdOrderByCreatedAtDesc(
            UUID userId
    );
}