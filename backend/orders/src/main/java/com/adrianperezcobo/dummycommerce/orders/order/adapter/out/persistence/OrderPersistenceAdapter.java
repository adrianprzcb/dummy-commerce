package com.adrianperezcobo.dummycommerce.orders.order.adapter.out.persistence;

import com.adrianperezcobo.dummycommerce.orders.order.application.port.out.OrderRepository;
import com.adrianperezcobo.dummycommerce.orders.order.domain.Order;
import com.adrianperezcobo.dummycommerce.orders.order.domain.OrderItem;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class OrderPersistenceAdapter
        implements OrderRepository {

    private final OrderJpaRepository repository;

    public OrderPersistenceAdapter(
            OrderJpaRepository repository
    ) {
        this.repository = repository;
    }

    @Override
    public Order save(Order order) {
        OrderJpaEntity entity =
                toEntity(order);

        return toDomain(
                repository.save(entity)
        );
    }

    @Override
    public Optional<Order> findById(
            UUID orderId
    ) {
        return repository
                .findByIdWithItems(orderId)
                .map(this::toDomain);
    }

    @Override
    public Optional<Order> findByIdForUpdate(
            UUID orderId
    ) {
        return repository
                .findByIdForUpdate(orderId)
                .map(this::toDomain);
    }

    @Override
    public List<Order> findByUserId(
            UUID userId
    ) {
        return repository
                .findByUserIdOrderByCreatedAtDesc(
                        userId
                )
                .stream()
                .map(this::toDomain)
                .toList();
    }

    private OrderJpaEntity toEntity(
            Order order
    ) {
        OrderJpaEntity entity =
                new OrderJpaEntity(
                        order.getId(),
                        order.getUserId(),
                        order.getStatus(),
                        order.getCreatedAt()
                );

        for (OrderItem item : order.getItems()) {
            entity.addItem(
                    new OrderItemJpaEntity(
                            item.getId(),
                            item.getProductId(),
                            item.getQuantity(),
                            item.getUnitPrice()
                    )
            );
        }

        return entity;
    }

    private Order toDomain(
            OrderJpaEntity entity
    ) {
        List<OrderItem> items =
                entity.getItems()
                        .stream()
                        .map(this::toDomain)
                        .toList();

        return new Order(
                entity.getId(),
                entity.getUserId(),
                items,
                entity.getStatus(),
                entity.getCreatedAt()
        );
    }

    private OrderItem toDomain(
            OrderItemJpaEntity entity
    ) {
        return new OrderItem(
                entity.getId(),
                entity.getProductId(),
                entity.getQuantity(),
                entity.getUnitPrice()
        );
    }
}