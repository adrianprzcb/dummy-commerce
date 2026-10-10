package com.adrianperezcobo.dummycommerce.payments.shared.kafka;

import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.*;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.web.server.ResponseStatusException;
import java.time.Duration;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DltRecordReaderTest {
    ConsumerFactory<String, String> factory;
    Consumer<String, String> consumer;
    DltRecordReader reader;
    TopicPartition target = new TopicPartition("test.DLT", 0);

    @BeforeEach
    void setup() {
        factory = mock(ConsumerFactory.class);
        consumer = mock(Consumer.class);
        reader = new DltRecordReader(factory, 1000);
        when(factory.createConsumer(anyString(), anyString(), isNull(), any(Properties.class))).thenReturn(consumer);
        when(consumer.beginningOffsets(anyCollection(), any(Duration.class))).thenReturn(Map.of(target, 2L));
        when(consumer.endOffsets(anyCollection(), any(Duration.class))).thenReturn(Map.of(target, 9L));
    }

    @Test
    void readsOnlyRequestedOffsetWithoutCommittingConsumerPosition() {
        var record = new ConsumerRecord<String, String>("test.DLT", 0, 7, "key", "payload");
        when(consumer.poll(any(Duration.class))).thenReturn(new ConsumerRecords<>(Map.of(target, List.of(record)), Map.of()));
        assertThat(reader.read("test.DLT", 0, 7)).isSameAs(record);
        verify(consumer).seek(target, 7);
        verify(consumer).close();
        verify(consumer, never()).commitSync();
    }

    @Test
    void expiredOffsetIsNotReplacedByAnotherRecord() {
        assertThatThrownBy(() -> reader.read("test.DLT", 0, 1)).isInstanceOf(ResponseStatusException.class);
        verify(consumer, never()).poll(any(Duration.class));
        verify(consumer).close();
    }
}
