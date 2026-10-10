package com.adrianperezcobo.dummycommerce.payments.shared.kafka;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.springframework.beans.factory.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class DltReprocessor {
    public record Result(String dltTopic, int partition, long offset, String originalTopic, Instant replayedAt) {}
    private final JdbcTemplate jdbc;
    private final DltRecordReader reader;
    private final KafkaTemplate<String, String> producer;
    private final Set<String> originalTopics;
    private final long sendTimeoutMs;

    public DltReprocessor(JdbcTemplate jdbc, DltRecordReader reader, KafkaTemplate<String, String> producer,
            @Qualifier("dltOriginalTopics") Set<String> originalTopics,
            @Value("${messaging.kafka.dlt.send-timeout-ms:10000}") long sendTimeoutMs) {
        if (sendTimeoutMs < 1) throw new IllegalArgumentException("Invalid DLT send timeout");
        this.jdbc = jdbc;
        this.reader = reader;
        this.producer = producer;
        this.originalTopics = originalTopics;
        this.sendTimeoutMs = sendTimeoutMs;
    }

    @Transactional
    public Result reprocess(String dltTopic, int partition, long offset) {
        String original = originalTopics.stream().filter(topic -> KafkaTopics.deadLetterTopic(topic).equals(dltTopic))
                .findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported DLT topic"));
        if (partition < 0 || offset < 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid DLT coordinates");
        jdbc.update("INSERT INTO dlt_replays (dlt_topic, partition_id, record_offset) VALUES (?, ?, ?) ON CONFLICT DO NOTHING",
                dltTopic, partition, offset);
        Timestamp previous = jdbc.queryForObject(
                "SELECT replayed_at FROM dlt_replays WHERE dlt_topic=? AND partition_id=? AND record_offset=? FOR UPDATE",
                Timestamp.class, dltTopic, partition, offset);
        if (previous != null) return new Result(dltTopic, partition, offset, original, previous.toInstant());
        try {
            var record = reader.read(dltTopic, partition, offset);
            var originalHeader = record.headers().lastHeader("kafka_dlt-original-topic");
            if (originalHeader == null || !original.equals(new String(originalHeader.value(), StandardCharsets.UTF_8))) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "DLT original topic does not match");
            }
            var headers = new RecordHeaders();
            record.headers().forEach(header -> {
                if (!header.key().startsWith("kafka_dlt-") && !header.key().startsWith("dummy-commerce-dlt-")) {
                    headers.add(header);
                }
            });
            headers.add("dummy-commerce-dlt-topic", dltTopic.getBytes(StandardCharsets.UTF_8));
            headers.add("dummy-commerce-dlt-coordinate", (partition + ":" + offset).getBytes(StandardCharsets.UTF_8));
            producer.send(new ProducerRecord<>(original, null, record.key(), record.value(), headers))
                    .get(sendTimeoutMs, TimeUnit.MILLISECONDS);
            Instant replayedAt = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
            jdbc.update("UPDATE dlt_replays SET replayed_at=? WHERE dlt_topic=? AND partition_id=? AND record_offset=?",
                    Timestamp.from(replayedAt), dltTopic, partition, offset);
            return new Result(dltTopic, partition, offset, original, replayedAt);
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "DLT replay interrupted", ex);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "DLT replay could not be published", ex);
        }
    }
}
