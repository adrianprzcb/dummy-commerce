package com.adrianperezcobo.dummycommerce.notifications.shared.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.mockito.ArgumentCaptor;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
class DltReprocessorIntegrationTest {
    @Container @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");
    @Autowired DltReprocessor reprocessor;
    @Autowired JdbcTemplate jdbc;
    @Autowired @Qualifier("dltOriginalTopics") Set<String> topics;
    @MockitoBean DltRecordReader reader;
    @MockitoBean KafkaTemplate<String, String> producer;
    String original;
    String dlt;

    @BeforeEach
    void setup() {
        jdbc.update("DELETE FROM dlt_replays");
        original = topics.iterator().next();
        dlt = KafkaTopics.deadLetterTopic(original);
    }

    private ConsumerRecord<String, String> record() {
        var record = new ConsumerRecord<String, String>(dlt, 0, 7, "order-key", "{\"messageId\":\"unchanged\"}");
        record.headers().add("kafka_dlt-original-topic", original.getBytes(StandardCharsets.UTF_8));
        record.headers().add("kafka_dlt-exception-message", "old-error".getBytes(StandardCharsets.UTF_8));
        record.headers().add("business-header", "preserved".getBytes(StandardCharsets.UTF_8));
        return record;
    }

    @Test
    void replayPreservesBusinessMessageAndOnlyPublishesOnceForSameCoordinate() {
        when(reader.read(dlt, 0, 7)).thenReturn(record());
        when(producer.send(any(ProducerRecord.class))).thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));
        var first = reprocessor.reprocess(dlt, 0, 7);
        var second = reprocessor.reprocess(dlt, 0, 7);
        assertThat(second).isEqualTo(first);
        var captor = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(producer, times(1)).send(captor.capture());
        var sent = captor.getValue();
        assertThat(sent.topic()).isEqualTo(original);
        assertThat(sent.key()).isEqualTo("order-key");
        assertThat(sent.value()).isEqualTo(record().value());
        assertThat(sent.headers().lastHeader("business-header").value()).isEqualTo("preserved".getBytes(StandardCharsets.UTF_8));
        assertThat(sent.headers().lastHeader("kafka_dlt-exception-message")).isNull();
        assertThat(first.replayedAt()).isNotNull();
    }

    @Test
    void failedAcknowledgementRollsBackReplayAndAllowsExplicitRetry() {
        when(reader.read(dlt, 0, 7)).thenReturn(record());
        when(producer.send(any(ProducerRecord.class))).thenReturn(CompletableFuture.failedFuture(new IllegalStateException("broker unavailable")));
        assertThatThrownBy(() -> reprocessor.reprocess(dlt, 0, 7)).isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(502));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM dlt_replays", Integer.class)).isZero();
        when(producer.send(any(ProducerRecord.class))).thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));
        assertThat(reprocessor.reprocess(dlt, 0, 7).replayedAt()).isNotNull();
    }

    @Test
    void rejectsAnotherConsumersTopic() {
        assertThatThrownBy(() -> reprocessor.reprocess("arbitrary-topic.DLT", 0, 0)).isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(reader);
        verify(producer, never()).send(any(ProducerRecord.class));
    }

    @Test
    void mismatchedOriginalHeaderCannotRedirectReplay() {
        var record = record();
        record.headers().remove("kafka_dlt-original-topic");
        record.headers().add("kafka_dlt-original-topic", "another-topic".getBytes(StandardCharsets.UTF_8));
        when(reader.read(dlt, 0, 7)).thenReturn(record);
        assertThatThrownBy(() -> reprocessor.reprocess(dlt, 0, 7)).isInstanceOf(ResponseStatusException.class);
        verify(producer, never()).send(any(ProducerRecord.class));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM dlt_replays", Integer.class)).isZero();
    }
}
