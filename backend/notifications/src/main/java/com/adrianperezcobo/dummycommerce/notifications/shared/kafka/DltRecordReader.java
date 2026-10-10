package com.adrianperezcobo.dummycommerce.notifications.shared.kafka;

import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.time.Duration;
import java.util.*;

@Component
public class DltRecordReader {
    private final ConsumerFactory<String, String> consumers;
    private final long timeoutMs;

    public DltRecordReader(ConsumerFactory<String, String> consumers,
            @Value("${messaging.kafka.dlt.read-timeout-ms:5000}") long timeoutMs) {
        if (timeoutMs < 1) throw new IllegalArgumentException("Invalid DLT read timeout");
        this.consumers = consumers;
        this.timeoutMs = timeoutMs;
    }

    public ConsumerRecord<String, String> read(String topic, int partition, long offset) {
        var overrides = new Properties();
        overrides.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        overrides.put(ConsumerConfig.ALLOW_AUTO_CREATE_TOPICS_CONFIG, false);
        overrides.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 1);
        overrides.put(ConsumerConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, Math.toIntExact(timeoutMs));
        try (var consumer = consumers.createConsumer("dlt-reprocess-" + UUID.randomUUID(), "dlt-reprocess-", null, overrides)) {
            var target = new TopicPartition(topic, partition);
            var targets = List.of(target);
            var duration = Duration.ofMillis(timeoutMs);
            long first = consumer.beginningOffsets(targets, duration).get(target);
            long end = consumer.endOffsets(targets, duration).get(target);
            if (offset < first || offset >= end) throw missing();
            consumer.assign(targets);
            consumer.seek(target, offset);
            long deadline = System.nanoTime() + duration.toNanos();
            while (System.nanoTime() < deadline) {
                var records = consumer.poll(Duration.ofNanos(Math.max(1, deadline - System.nanoTime())));
                for (var record : records.records(target)) {
                    if (record.offset() == offset) return record;
                    if (record.offset() > offset) throw missing();
                }
            }
            throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "DLT record read timed out");
        }
    }

    private ResponseStatusException missing() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "DLT record not found");
    }
}
