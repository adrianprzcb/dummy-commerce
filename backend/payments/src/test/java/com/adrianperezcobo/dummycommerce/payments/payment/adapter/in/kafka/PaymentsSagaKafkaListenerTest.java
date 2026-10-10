package com.adrianperezcobo.dummycommerce.payments.payment.adapter.in.kafka;

import com.adrianperezcobo.dummycommerce.payments.payment.application.integration.payments.*;
import com.adrianperezcobo.dummycommerce.payments.payment.application.service.PaymentSagaHandler;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class PaymentsSagaKafkaListenerTest {
    private final JsonMapper json = JsonMapper.builder().build();
    private final PaymentSagaHandler handler = mock(PaymentSagaHandler.class);
    private final PaymentsSagaKafkaListener listener = new PaymentsSagaKafkaListener(json, handler);

    @Test
    void deserializesProcessPaymentCommandV1AndDelegates() {
        UUID orderId = UUID.randomUUID();
        var message = new ProcessPaymentCommandV1(UUID.randomUUID(), orderId, Instant.now(), new BigDecimal("24.68"), "EUR");
        var record = new ConsumerRecord<String, String>(PaymentsTopics.PROCESS_PAYMENT_V1, 0, 0, orderId.toString(), json.writeValueAsString(message));
        listener.onProcessPaymentCommandV1(record);
        verify(handler).handle(message);
    }

    @Test
    void rejectsMismatchedKeyAndMalformedJson() {
        UUID orderId = UUID.randomUUID();
        var message = new ProcessPaymentCommandV1(UUID.randomUUID(), orderId, Instant.now(), new BigDecimal("24.68"), "EUR");
        assertThatThrownBy(() -> listener.onProcessPaymentCommandV1(new ConsumerRecord<>(PaymentsTopics.PROCESS_PAYMENT_V1, 0, 0, "wrong-key", json.writeValueAsString(message))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> listener.onProcessPaymentCommandV1(new ConsumerRecord<>(PaymentsTopics.PROCESS_PAYMENT_V1, 0, 0, orderId.toString(), "invalid-json")))
                .isInstanceOf(RuntimeException.class);
        verifyNoInteractions(handler);
    }

}
