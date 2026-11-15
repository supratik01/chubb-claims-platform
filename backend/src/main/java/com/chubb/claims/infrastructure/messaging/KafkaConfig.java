package com.chubb.claims.infrastructure.messaging;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;
import tools.jackson.core.JacksonException;

@Configuration
class KafkaConfig {

    private static final int PARTITIONS = 3;
    private static final long RETRY_INTERVAL_MS = 1_000;
    private static final long MAX_RETRIES = 3;

    @Bean
    NewTopic claimEventsTopic(MessagingProperties properties) {
        return TopicBuilder.name(properties.topic()).partitions(PARTITIONS).replicas(1).build();
    }

    @Bean
    NewTopic claimEventsDeadLetterTopic(MessagingProperties properties) {
        return TopicBuilder.name(properties.deadLetterTopic()).partitions(1).replicas(1).build();
    }

    /**
     * A failing event is retried three times, then parked on the dead-letter topic so that one bad message
     * cannot block the partition. Malformed JSON will never succeed, so it is not retried.
     */
    @Bean
    CommonErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> kafka, MessagingProperties properties) {
        var recoverer = new DeadLetterPublishingRecoverer(kafka,
                (failedRecord, exception) -> new TopicPartition(properties.deadLetterTopic(), 0));
        var handler = new DefaultErrorHandler(recoverer, new FixedBackOff(RETRY_INTERVAL_MS, MAX_RETRIES));
        handler.addNotRetryableExceptions(JacksonException.class);
        return handler;
    }
}
