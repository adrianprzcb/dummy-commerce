package com.adrianperezcobo.dummycommerce.payments.payment.adapter.out.processor;

import com.adrianperezcobo.dummycommerce.payments.payment.application.port.out.PaymentProcessorPort;
import com.adrianperezcobo.dummycommerce.payments.payment.application.port.out.PaymentProcessorResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.UUID;

@Component
public class SimulatedPaymentProcessorAdapter
        implements PaymentProcessorPort {

    private final PaymentSimulatorProperties properties;

    public SimulatedPaymentProcessorAdapter(
            PaymentSimulatorProperties properties
    ) {
        this.properties = properties;
    }

    @Override
    public PaymentProcessorResult process(
            UUID paymentId,
            UUID orderId,
            BigDecimal amount,
            String currency
    ) {
        return configuredResult();
    }

    @Override
    public PaymentProcessorResult refund(
            UUID paymentId,
            UUID orderId,
            BigDecimal amount,
            String currency
    ) {
        return configuredResult();
    }

    private PaymentProcessorResult configuredResult() {
        String mode = properties.mode();

        if (mode == null) {
            throw new IllegalStateException(
                    "Payment simulator mode is required"
            );
        }

        return switch (
                mode.trim()
                        .toLowerCase(Locale.ROOT)
                ) {
            case "success" ->
                    PaymentProcessorResult.SUCCESS;

            case "failure" ->
                    PaymentProcessorResult.FAILURE;

            default ->
                    throw new IllegalStateException(
                            "Unsupported payment simulator mode: "
                                    + mode
                    );
        };
    }
}