package com.adrianperezcobo.dummycommerce.payments.payment.adapter.in.kafka;

import com.adrianperezcobo.dummycommerce.payments.payment.application.integration.payments.*;
import com.adrianperezcobo.dummycommerce.payments.payment.application.service.PaymentSagaHandler;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;
import java.util.UUID;

@Component
public class PaymentsSagaKafkaListener {
    private final JsonMapper jsonMapper;
    private final PaymentSagaHandler handler;

    public PaymentsSagaKafkaListener(JsonMapper jsonMapper, PaymentSagaHandler handler) {
        this.jsonMapper = jsonMapper;
        this.handler = handler;
    }

    @KafkaListener(topics = PaymentsTopics.PROCESS_PAYMENT_V1)
    public void onProcessPaymentCommandV1(ConsumerRecord<String, String> record) {
        ProcessPaymentCommandV1 message = jsonMapper.readValue(record.value(), ProcessPaymentCommandV1.class);
        requireOrderKey(record.key(), message.orderId());
        handler.handle(message);
    }

    @KafkaListener(topics = PaymentsTopics.REFUND_PAYMENT_V1)
    public void onRefundPaymentCommandV1(ConsumerRecord<String, String> record) {
        RefundPaymentCommandV1 message = jsonMapper.readValue(record.value(), RefundPaymentCommandV1.class);
        requireOrderKey(record.key(), message.orderId());
        handler.handle(message);
    }

    private void requireOrderKey(String key, UUID orderId) {
        if (!orderId.toString().equals(key)) throw new IllegalArgumentException("Kafka key must match orderId");
    }
}
