package com.adrianperezcobo.dummycommerce.payments.shared.kafka;

import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.producer.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.*;
import org.springframework.kafka.support.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class KafkaErrorHandlerTest {
    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, String> template = mock(KafkaTemplate.class);
    @SuppressWarnings("unchecked")
    private final Consumer<String, String> consumer = mock(Consumer.class);
    private final MessageListenerContainer container = mock(MessageListenerContainer.class);
    private final ConsumerRecord<String, String> record = new ConsumerRecord<>("original", 0, 7, "order-key", "payload");
    private final KafkaConfiguration configuration = new KafkaConfiguration();

    @Test
    void retriesTechnicalFailuresThreeTimesBeforeRecovery() {
        DeadLetterPublishingRecoverer recoverer = mock(DeadLetterPublishingRecoverer.class);
        var handler = configuration.kafkaErrorHandler(recoverer, 0, 3);
        var error = new IllegalStateException("temporarily unavailable");
        for (int attempt = 0; attempt < 3; attempt++) {
            assertThat(handler.handleOne(error, record, consumer, container)).isFalse();
        }
        verifyNoInteractions(recoverer);
        assertThat(handler.handleOne(error, record, consumer, container)).isTrue();
        verify(recoverer).accept(record, consumer, error);
    }

    @Test
    void invalidContractGoesDirectlyToDeadLetterTopic() {
        var recoverer = mock(DeadLetterPublishingRecoverer.class);
        var handler = configuration.kafkaErrorHandler(recoverer, 0, 3);
        var error = new IllegalArgumentException("invalid contract");
        assertThat(handler.handleOne(error, record, consumer, container)).isTrue();
        verify(recoverer).accept(record, consumer, error);
    }

    @Test
    void preservesOriginalKeyPayloadAndTopicHeader() {
        record.headers().add("business-header", new byte[] {1, 2});
        when(template.send(any(ProducerRecord.class))).thenAnswer(invocation -> CompletableFuture.completedFuture(
                new SendResult<>(invocation.getArgument(0), mock(RecordMetadata.class))));
        var recoverer = configuration.deadLetterRecoverer(template);
        recoverer.setVerifyPartition(false);
        recoverer.accept(record, consumer, new IllegalArgumentException("invalid contract"));
        var captured = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(template).send(captured.capture());
        ProducerRecord<?, ?> sent = captured.getValue();
        assertThat(sent.topic()).isEqualTo("original.DLT");
        assertThat(sent.key()).isEqualTo(record.key());
        assertThat(sent.value()).isEqualTo(record.value());
        assertThat(sent.headers().lastHeader("business-header").value()).containsExactly(1, 2);
        assertThat(new String(sent.headers().lastHeader(KafkaHeaders.DLT_ORIGINAL_TOPIC).value(), StandardCharsets.UTF_8))
                .isEqualTo("original");
    }

    @Test
    void failedDltPublicationDoesNotAcknowledgeRecovery() {
        when(template.send(any(ProducerRecord.class))).thenReturn(
                CompletableFuture.failedFuture(new IllegalStateException("broker unavailable")));
        var recoverer = configuration.deadLetterRecoverer(template);
        recoverer.setVerifyPartition(false);
        var handler = configuration.kafkaErrorHandler(recoverer, 0, 0);
        assertThat(handler.handleOne(new IllegalArgumentException("invalid"), record, consumer, container)).isFalse();
    }
}
