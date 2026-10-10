package com.adrianperezcobo.dummycommerce.orders.order.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.List;
import java.time.Instant;
import com.adrianperezcobo.dummycommerce.orders.order.domain.CompensationStatus;

public interface OrderCompensationJpaRepository extends JpaRepository<OrderCompensationJpaEntity, UUID> {
    List<OrderCompensationJpaEntity> findTop100ByStatusAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc(
            CompensationStatus status, Instant now);
}
