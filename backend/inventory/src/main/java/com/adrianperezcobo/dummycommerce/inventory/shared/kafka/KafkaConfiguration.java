package com.adrianperezcobo.dummycommerce.inventory.shared.kafka;

import com.adrianperezcobo.dummycommerce.inventory.reservation.application.integration.inventory.InventoryTopics;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.listener.*;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.util.backoff.FixedBackOff;
import tools.jackson.core.JacksonException;
import java.util.stream.Stream;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
public class KafkaConfiguration {
    @Bean
    DeadLetterPublishingRecoverer deadLetterRecoverer(KafkaTemplate<String, String> template) {
        var recoverer = new DeadLetterPublishingRecoverer(template,
                (record, exception) -> new TopicPartition(KafkaTopics.deadLetterTopic(record.topic()), -1));
        recoverer.setFailIfSendResultIsError(true);
        return recoverer;
    }

    @Bean
    DefaultErrorHandler kafkaErrorHandler(DeadLetterPublishingRecoverer recoverer,
            @Value("${messaging.kafka.retry.backoff-ms:1000}") long backoffMs,
            @Value("${messaging.kafka.retry.max-retries:3}") long maxRetries) {
        if (backoffMs < 0 || maxRetries < 0) throw new IllegalArgumentException("Invalid Kafka retry policy");
        var handler = new DefaultErrorHandler(recoverer, new FixedBackOff(backoffMs, maxRetries));
        handler.addNotRetryableExceptions(JacksonException.class, IllegalArgumentException.class, NullPointerException.class);
        return handler;
    }

    @Bean
    KafkaAdmin.NewTopics deadLetterTopics() {
        return new KafkaAdmin.NewTopics(dltOriginalTopics().stream()
                .map(KafkaTopics::deadLetterTopic)
                .map(topic -> TopicBuilder.name(topic).partitions(1).replicas(1).build())
                .toArray(org.apache.kafka.clients.admin.NewTopic[]::new));
    }
    @Bean
    java.util.Set<String> dltOriginalTopics() {
        return java.util.Set.of(
                InventoryTopics.RESERVE_STOCK_V1,
                InventoryTopics.CONFIRM_STOCK_V1,
                InventoryTopics.RELEASE_STOCK_V1);
    }
}
