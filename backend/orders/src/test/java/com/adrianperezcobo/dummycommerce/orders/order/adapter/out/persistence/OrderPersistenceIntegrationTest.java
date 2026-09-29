package com.adrianperezcobo.dummycommerce.orders.order.adapter.out.persistence;

import com.adrianperezcobo.dummycommerce.orders.order.application.port.out.OrderRepository;
import com.adrianperezcobo.dummycommerce.orders.order.domain.Order;
import com.adrianperezcobo.dummycommerce.orders.order.domain.OrderItem;
import com.adrianperezcobo.dummycommerce.orders.order.domain.OrderStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.NONE;

@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@Import(OrderPersistenceAdapter.class)
@Testcontainers
class OrderPersistenceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer(
                    "postgres:17-alpine"
            );

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldPersistAndLoadCompleteOrderAggregate() {
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        UUID firstProductId = UUID.randomUUID();
        UUID secondProductId = UUID.randomUUID();

        Order order = new Order(
                orderId,
                userId,
                List.of(
                        new OrderItem(
                                UUID.randomUUID(),
                                firstProductId,
                                2,
                                new BigDecimal("19.99")
                        ),
                        new OrderItem(
                                UUID.randomUUID(),
                                secondProductId,
                                3,
                                new BigDecimal("5.50")
                        )
                ),
                OrderStatus.CREATED,
                Instant.now()
        );

        orderRepository.save(order);

        flushAndClear();

        Order loaded =
                orderRepository
                        .findById(orderId)
                        .orElseThrow();

        assertThat(loaded.getId())
                .isEqualTo(orderId);

        assertThat(loaded.getUserId())
                .isEqualTo(userId);

        assertThat(loaded.getStatus())
                .isEqualTo(OrderStatus.CREATED);

        assertThat(loaded.getItems())
                .hasSize(2);

        assertThat(loaded.getTotalAmount())
                .isEqualByComparingTo("56.48");

        OrderItem firstItem =
                loaded.getItems()
                        .stream()
                        .filter(item ->
                                item.getProductId()
                                        .equals(firstProductId)
                        )
                        .findFirst()
                        .orElseThrow();

        assertThat(firstItem.getQuantity())
                .isEqualTo(2);

        assertThat(firstItem.getUnitPrice())
                .isEqualByComparingTo("19.99");

        assertThat(firstItem.getSubtotal())
                .isEqualByComparingTo("39.98");
    }

    @Test
    void shouldFindOrdersByUserOrderedByNewestFirst() {
        UUID userId = UUID.randomUUID();

        Order older = createOrder(
                userId,
                Instant.parse(
                        "2026-01-01T10:00:00Z"
                )
        );

        Order newer = createOrder(
                userId,
                Instant.parse(
                        "2026-01-02T10:00:00Z"
                )
        );

        orderRepository.save(older);
        orderRepository.save(newer);

        flushAndClear();

        List<Order> orders =
                orderRepository.findByUserId(
                        userId
                );

        assertThat(orders)
                .hasSize(2);

        assertThat(orders.get(0).getId())
                .isEqualTo(newer.getId());

        assertThat(orders.get(1).getId())
                .isEqualTo(older.getId());
    }

    @Test
    void shouldPersistOrderStatusChange() {
        Order order = createOrder(
                UUID.randomUUID(),
                Instant.now()
        );

        orderRepository.save(order);

        flushAndClear();

        Order loaded =
                orderRepository
                        .findByIdForUpdate(
                                order.getId()
                        )
                        .orElseThrow();

        loaded.markStockReserved();

        orderRepository.save(loaded);

        flushAndClear();

        Order updated =
                orderRepository
                        .findById(order.getId())
                        .orElseThrow();

        assertThat(updated.getStatus())
                .isEqualTo(
                        OrderStatus.STOCK_RESERVED
                );

        assertThat(updated.getItems())
                .hasSize(1);
    }

    @Test
    void shouldLoadOrderForUpdate() {
        Order order = createOrder(
                UUID.randomUUID(),
                Instant.now()
        );

        orderRepository.save(order);

        flushAndClear();

        Order loaded =
                orderRepository
                        .findByIdForUpdate(
                                order.getId()
                        )
                        .orElseThrow();

        assertThat(loaded.getId())
                .isEqualTo(order.getId());

        assertThat(loaded.getStatus())
                .isEqualTo(OrderStatus.CREATED);

        assertThat(loaded.getItems())
                .hasSize(1);
    }

    @Test
    void shouldReturnEmptyForUnknownOrder() {
        assertThat(
                orderRepository.findById(
                        UUID.randomUUID()
                )
        ).isEmpty();
    }

    private Order createOrder(
            UUID userId,
            Instant createdAt
    ) {
        return new Order(
                UUID.randomUUID(),
                userId,
                List.of(
                        new OrderItem(
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                1,
                                new BigDecimal("10.00")
                        )
                ),
                OrderStatus.CREATED,
                createdAt
        );
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}