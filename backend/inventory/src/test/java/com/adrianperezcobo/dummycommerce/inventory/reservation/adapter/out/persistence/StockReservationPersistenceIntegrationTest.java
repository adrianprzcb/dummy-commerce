package com.adrianperezcobo.dummycommerce.inventory.reservation.adapter.out.persistence;

import com.adrianperezcobo.dummycommerce.inventory.reservation.application.port.out.StockReservationRepository;
import com.adrianperezcobo.dummycommerce.inventory.reservation.domain.ReservationStatus;
import com.adrianperezcobo.dummycommerce.inventory.reservation.domain.StockReservation;
import com.adrianperezcobo.dummycommerce.inventory.stock.adapter.out.persistence.InventoryPersistenceAdapter;
import com.adrianperezcobo.dummycommerce.inventory.stock.application.port.out.InventoryRepository;
import com.adrianperezcobo.dummycommerce.inventory.stock.domain.InventoryItem;
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

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.NONE;

@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@Import({
        InventoryPersistenceAdapter.class,
        StockReservationPersistenceAdapter.class
})
@Testcontainers
class StockReservationPersistenceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer(
                    "postgres:17-alpine"
            );

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private StockReservationRepository reservationRepository;

    @Test
    void shouldPersistAndLoadReservation() {
        UUID productId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID reservationId = UUID.randomUUID();

        createInventory(productId);

        StockReservation reservation =
                new StockReservation(
                        reservationId,
                        orderId,
                        productId,
                        3,
                        ReservationStatus.RESERVED,
                        Instant.now()
                );

        reservationRepository.save(reservation);

        StockReservation loaded =
                reservationRepository
                        .findById(reservationId)
                        .orElseThrow();

        assertThat(loaded.getId())
                .isEqualTo(reservationId);

        assertThat(loaded.getOrderId())
                .isEqualTo(orderId);

        assertThat(loaded.getProductId())
                .isEqualTo(productId);

        assertThat(loaded.getQuantity())
                .isEqualTo(3);

        assertThat(loaded.getStatus())
                .isEqualTo(
                        ReservationStatus.RESERVED
                );
    }

    @Test
    void shouldFindReservationByOrderAndProduct() {
        UUID productId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        createInventory(productId);

        reservationRepository.save(
                new StockReservation(
                        UUID.randomUUID(),
                        orderId,
                        productId,
                        2,
                        ReservationStatus.RESERVED,
                        Instant.now()
                )
        );

        StockReservation loaded =
                reservationRepository
                        .findByOrderIdAndProductId(
                                orderId,
                                productId
                        )
                        .orElseThrow();

        assertThat(loaded.getOrderId())
                .isEqualTo(orderId);

        assertThat(loaded.getProductId())
                .isEqualTo(productId);

        assertThat(
                reservationRepository
                        .existsByOrderIdAndProductId(
                                orderId,
                                productId
                        )
        ).isTrue();
    }

    @Test
    void shouldPersistReservationStatusChange() {
        UUID productId = UUID.randomUUID();

        createInventory(productId);

        StockReservation reservation =
                new StockReservation(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        productId,
                        2,
                        ReservationStatus.RESERVED,
                        Instant.now()
                );

        reservation =
                reservationRepository.save(
                        reservation
                );

        reservation.confirm();

        reservationRepository.save(
                reservation
        );

        StockReservation loaded =
                reservationRepository
                        .findById(
                                reservation.getId()
                        )
                        .orElseThrow();

        assertThat(loaded.getStatus())
                .isEqualTo(
                        ReservationStatus.CONFIRMED
                );
    }

    @Test
    void shouldLoadReservationForUpdate() {
        UUID productId = UUID.randomUUID();

        createInventory(productId);

        StockReservation reservation =
                reservationRepository.save(
                        new StockReservation(
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                productId,
                                1,
                                ReservationStatus.RESERVED,
                                Instant.now()
                        )
                );

        StockReservation loaded =
                reservationRepository
                        .findByIdForUpdate(
                                reservation.getId()
                        )
                        .orElseThrow();

        assertThat(loaded.getId())
                .isEqualTo(
                        reservation.getId()
                );

        assertThat(loaded.getStatus())
                .isEqualTo(
                        ReservationStatus.RESERVED
                );
    }

    private void createInventory(
            UUID productId
    ) {
        inventoryRepository.save(
                new InventoryItem(
                        productId,
                        10,
                        0
                )
        );
    }
}