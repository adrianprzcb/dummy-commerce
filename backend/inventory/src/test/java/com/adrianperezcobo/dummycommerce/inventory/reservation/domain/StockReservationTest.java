package com.adrianperezcobo.dummycommerce.inventory.reservation.domain;

import com.adrianperezcobo.dummycommerce.inventory.reservation.domain.exception.InvalidReservationStateException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StockReservationTest {

    @Test
    void shouldCreateReservedReservation() {
        StockReservation reservation =
                createReservation(
                        ReservationStatus.RESERVED
                );

        assertThat(reservation.getStatus())
                .isEqualTo(
                        ReservationStatus.RESERVED
                );
    }

    @Test
    void shouldConfirmReservedReservation() {
        StockReservation reservation =
                createReservation(
                        ReservationStatus.RESERVED
                );

        reservation.confirm();

        assertThat(reservation.getStatus())
                .isEqualTo(
                        ReservationStatus.CONFIRMED
                );
    }

    @Test
    void shouldReleaseReservedReservation() {
        StockReservation reservation =
                createReservation(
                        ReservationStatus.RESERVED
                );

        reservation.release();

        assertThat(reservation.getStatus())
                .isEqualTo(
                        ReservationStatus.RELEASED
                );
    }

    @Test
    void shouldNotConfirmAlreadyConfirmedReservation() {
        StockReservation reservation =
                createReservation(
                        ReservationStatus.CONFIRMED
                );

        assertThatThrownBy(
                reservation::confirm
        ).isInstanceOf(
                InvalidReservationStateException.class
        );
    }

    @Test
    void shouldNotReleaseConfirmedReservation() {
        StockReservation reservation =
                createReservation(
                        ReservationStatus.CONFIRMED
                );

        assertThatThrownBy(
                reservation::release
        ).isInstanceOf(
                InvalidReservationStateException.class
        );
    }

    @Test
    void shouldNotConfirmReleasedReservation() {
        StockReservation reservation =
                createReservation(
                        ReservationStatus.RELEASED
                );

        assertThatThrownBy(
                reservation::confirm
        ).isInstanceOf(
                InvalidReservationStateException.class
        );
    }

    @Test
    void shouldRejectNonPositiveQuantity() {
        assertThatThrownBy(() ->
                new StockReservation(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        0,
                        ReservationStatus.RESERVED,
                        Instant.now()
                )
        ).isInstanceOf(
                IllegalArgumentException.class
        );
    }

    private StockReservation createReservation(
            ReservationStatus status
    ) {
        return new StockReservation(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                3,
                status,
                Instant.now()
        );
    }
}