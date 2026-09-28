package com.adrianperezcobo.dummycommerce.inventory.shared.persistence;

import com.adrianperezcobo.dummycommerce.inventory.reservation.application.command.ReserveStockCommand;
import com.adrianperezcobo.dummycommerce.inventory.reservation.application.port.in.ReserveStockUseCase;
import com.adrianperezcobo.dummycommerce.inventory.stock.application.port.out.InventoryRepository;
import com.adrianperezcobo.dummycommerce.inventory.stock.domain.InventoryItem;
import com.adrianperezcobo.dummycommerce.inventory.stock.domain.exception.InsufficientStockException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

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
class InventoryConcurrencyIntegrationTest {

    private static final int INITIAL_STOCK = 5;
    private static final int CONCURRENT_REQUESTS = 20;

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer(
                    "postgres:17-alpine"
            );

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private ReserveStockUseCase reserveStockUseCase;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @Timeout(30)
    void shouldPreventOversellingWithConcurrentReservations()
            throws Exception {

        UUID productId = UUID.randomUUID();

        inventoryRepository.save(
                new InventoryItem(
                        productId,
                        INITIAL_STOCK,
                        0
                )
        );

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
            List<Future<ReservationAttempt>>
                    futures = new ArrayList<>();

            for (int i = 0;
                 i < CONCURRENT_REQUESTS;
                 i++) {

                UUID orderId = UUID.randomUUID();

                futures.add(
                        executor.submit(
                                reservationTask(
                                        orderId,
                                        productId,
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

            assertThat(allReady).isTrue();

            /*
             * Los 20 threads han llegado al punto
             * de salida.
             *
             * Los liberamos prácticamente al mismo
             * tiempo.
             */
            startLatch.countDown();

            List<ReservationAttempt> results =
                    new ArrayList<>();

            for (Future<ReservationAttempt> future
                    : futures) {

                results.add(
                        future.get(
                                15,
                                TimeUnit.SECONDS
                        )
                );
            }

            long successfulReservations =
                    results.stream()
                            .filter(
                                    result ->
                                            result ==
                                                    ReservationAttempt.SUCCESS
                            )
                            .count();

            long rejectedReservations =
                    results.stream()
                            .filter(
                                    result ->
                                            result ==
                                                    ReservationAttempt.INSUFFICIENT_STOCK
                            )
                            .count();

            assertThat(successfulReservations)
                    .isEqualTo(INITIAL_STOCK);

            assertThat(rejectedReservations)
                    .isEqualTo(
                            CONCURRENT_REQUESTS
                                    - INITIAL_STOCK
                    );

            InventoryItem finalInventory =
                    inventoryRepository
                            .findByProductId(productId)
                            .orElseThrow();

            assertThat(
                    finalInventory
                            .getAvailableQuantity()
            ).isZero();

            assertThat(
                    finalInventory
                            .getReservedQuantity()
            ).isEqualTo(INITIAL_STOCK);

            assertThat(
                    finalInventory
                            .getTotalQuantity()
            ).isEqualTo(INITIAL_STOCK);

            Long persistedReservations =
                    jdbcTemplate.queryForObject(
                            """
                            SELECT COUNT(*)
                            FROM stock_reservations
                            WHERE product_id = ?
                            """,
                            Long.class,
                            productId
                    );

            assertThat(persistedReservations)
                    .isEqualTo(INITIAL_STOCK);

        } finally {
            executor.shutdownNow();
        }
    }

    private Callable<ReservationAttempt>
    reservationTask(
            UUID orderId,
            UUID productId,
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
                reserveStockUseCase.reserve(
                        new ReserveStockCommand(
                                orderId,
                                productId,
                                1
                        )
                );

                return ReservationAttempt.SUCCESS;

            } catch (
                    InsufficientStockException exception
            ) {
                return ReservationAttempt
                        .INSUFFICIENT_STOCK;
            }
        };
    }

    private enum ReservationAttempt {
        SUCCESS,
        INSUFFICIENT_STOCK
    }
}