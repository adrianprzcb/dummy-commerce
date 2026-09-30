package com.adrianperezcobo.dummycommerce.payments.payment.adapter.out.processor;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(
        PaymentSimulatorProperties.class
)
public class PaymentProcessorConfig {
}