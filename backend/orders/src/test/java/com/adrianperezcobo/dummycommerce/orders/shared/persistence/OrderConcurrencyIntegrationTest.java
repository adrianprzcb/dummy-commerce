package com.adrianperezcobo.dummycommerce.orders.shared.persistence;

import com.adrianperezcobo.dummycommerce.orders.order.application.port.in.MarkStockReservedUseCase;
import com.adrianperezcobo.dummycommerce.orders.order.application.port.out.OrderRepository;
import com.adrianperezcobo.dummycommerce.orders.order.domain.Order;
import com.adrianperezcobo.dummycommerce.orders.order.domain.OrderItem;
import com.adrianperezcobo.dummycommerce.orders.order.domain.OrderStatus;
import com.adrianperezcobo.dummycommerce.orders.order.domain.exception.InvalidOrderStateException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@Testcontainers
class OrderConcurrencyIntegrationTest {

    private static final int CONCURRENT_REQUESTS = 10;

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer(
                    "postgres:17-alpine"
            );

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private MarkStockReservedUseCase
            markStockReservedUseCase;

    @Test
    @Timeout(30)
    void shouldAllowOnlyOneConcurrentStateTransition()
            throws Exception {

        Order order = createOrder();

        orderRepository.save(order);

        UUID orderId = order.getId();

        ExecutorService executor =
                Executors.newFixedThreadPool(
                        CONCURRENT_REQUESTS
                );

        CountDownLatch readyLatch =
                new CountDownLatch(
                        CONCURRENT_REQUESTS
                );

        CountDownLatch startLatch =
                new CountDownLatch(1);

        try {
            List<Future<TransitionAttempt>>
                    futures = new ArrayList<>();

            for (int i = 0;
                 i < CONCURRENT_REQUESTS;
                 i++) {

                futures.add(
                        executor.submit(
                                transitionTask(
                                        orderId,
                                        readyLatch,
                                        startLatch
                                )
                        )
                );
            }

            boolean allReady =
                    readyLatch.await(
                            10,
                            TimeUnit.SECONDS
                    );

            assertThat(allReady)
                    .isTrue();

            startLatch.countDown();

            List<TransitionAttempt> results =
                    new ArrayList<>();

            for (Future<TransitionAttempt> future
                    : futures) {

                results.add(
                        future.get(
                                15,
                                TimeUnit.SECONDS
                        )
                );
            }

            long successes =
                    results.stream()
                            .filter(
                                    attempt ->
                                            attempt ==
                                                    TransitionAttempt.SUCCESS
                            )
                            .count();

            long invalidTransitions =
                    results.stream()
                            .filter(
                                    attempt ->
                                            attempt ==
                                                    TransitionAttempt.INVALID_STATE
                            )
                            .count();

            assertThat(successes)
                    .isEqualTo(1);

            assertThat(invalidTransitions)
                    .isEqualTo(
                            CONCURRENT_REQUESTS - 1
                    );

            Order finalOrder =
                    orderRepository
                            .findById(orderId)
                            .orElseThrow();

            assertThat(finalOrder.getStatus())
                    .isEqualTo(
                            OrderStatus.STOCK_RESERVED
                    );

        } finally {
            executor.shutdownNow();
        }
    }

    private Callable<TransitionAttempt>
    transitionTask(
            UUID orderId,
            CountDownLatch readyLatch,
            CountDownLatch startLatch
    ) {
        return () -> {
            readyLatch.countDown();

            boolean started =
                    startLatch.await(
                            10,
                            TimeUnit.SECONDS
                    );

            if (!started) {
                throw new IllegalStateException(
                        "Concurrent test did not start in time"
                );
            }

            try {
                markStockReservedUseCase
                        .markStockReserved(
                                orderId
                        );

                return TransitionAttempt.SUCCESS;

            } catch (
                    InvalidOrderStateException exception
            ) {
                return TransitionAttempt.INVALID_STATE;
            }
        };
    }

    private Order createOrder() {
        return new Order(
                UUID.randomUUID(),
                UUID.randomUUID(),
                List.of(
                        new OrderItem(
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                1,
                                new BigDecimal("10.00")
                        )
                ),
                OrderStatus.CREATED,
                Instant.now()
        );
    }

    private enum TransitionAttempt {
        SUCCESS,
        INVALID_STATE
    }
}