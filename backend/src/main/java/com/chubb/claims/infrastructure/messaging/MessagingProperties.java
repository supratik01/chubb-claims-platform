package com.chubb.claims.infrastructure.messaging;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param topic            the topic claim events are published to
 * @param deadLetterTopic  where events go when a consumer cannot process them after retries
 * @param consumerGroup    the consumer group of the exposure projection
 * @param outboxBatchSize  how many outbox rows the relay publishes per run
 * @param sendTimeout      how long the relay waits for Kafka to acknowledge one event
 */
@ConfigurationProperties("claims.messaging")
public record MessagingProperties(String topic, String deadLetterTopic, String consumerGroup, int outboxBatchSize,
                                  Duration sendTimeout) {
}
