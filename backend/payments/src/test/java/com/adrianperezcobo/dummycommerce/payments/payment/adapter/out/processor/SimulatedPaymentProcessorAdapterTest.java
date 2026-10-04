package com.adrianperezcobo.dummycommerce.payments.payment.adapter.out.processor;

import com.adrianperezcobo.dummycommerce.payments.payment.application.port.out.PaymentProcessorResult;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SimulatedPaymentProcessorAdapterTest {

    @Test
    void shouldReturnSuccessWhenConfiguredForSuccess() {
        SimulatedPaymentProcessorAdapter adapter =
                adapter("success");

        assertThat(
                adapter.process(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        new BigDecimal("10.00"),
                        "EUR"
                )
        ).isEqualTo(
                PaymentProcessorResult.SUCCESS
        );
    }

    @Test
    void shouldReturnFailureWhenConfiguredForFailure() {
        SimulatedPaymentProcessorAdapter adapter =
                adapter("failure");

        assertThat(
                adapter.process(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        new BigDecimal("10.00"),
                        "EUR"
                )
        ).isEqualTo(
                PaymentProcessorResult.FAILURE
        );
    }

    @Test
    void shouldUseSameConfigurationForRefund() {
        SimulatedPaymentProcessorAdapter adapter =
                adapter("success");

        assertThat(
                adapter.refund(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        new BigDecimal("10.00"),
                        "EUR"
                )
        ).isEqualTo(
                PaymentProcessorResult.SUCCESS
        );
    }

    @Test
    void shouldNormalizeConfiguredMode() {
        SimulatedPaymentProcessorAdapter adapter =
                adapter("  SUCCESS ");

        assertThat(
                adapter.process(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        BigDecimal.ONE,
                        "EUR"
                )
        ).isEqualTo(
                PaymentProcessorResult.SUCCESS
        );
    }

    @Test
    void shouldRejectUnsupportedMode() {
        SimulatedPaymentProcessorAdapter adapter =
                adapter("random");

        assertThatThrownBy(() ->
                adapter.process(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        BigDecimal.ONE,
                        "EUR"
                )
        ).isInstanceOf(
                IllegalStateException.class
        );
    }

    @Test
    void shouldRejectNullMode() {
        SimulatedPaymentProcessorAdapter adapter =
                adapter(null);

        assertThatThrownBy(() ->
                adapter.process(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        BigDecimal.ONE,
                        "EUR"
                )
        ).isInstanceOf(
                IllegalStateException.class
        );
    }

    private SimulatedPaymentProcessorAdapter adapter(
            String mode
    ) {
        return new SimulatedPaymentProcessorAdapter(
                new PaymentSimulatorProperties(mode)
        );
    }
}