package com.adrianperezcobo.dummycommerce.payments.payment.adapter.out.processor;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(
        prefix = "payment.simulator"
)
public record PaymentSimulatorProperties(
        String mode
) {
}