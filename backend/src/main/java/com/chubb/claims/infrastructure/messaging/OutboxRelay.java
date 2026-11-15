package com.chubb.claims.infrastructure.messaging;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Publishes outbox rows to Kafka in the order they were written and marks them published.
 *
 * <p>If Kafka is unavailable the transaction rolls back and the rows are retried on the next run, so an
 * event is never lost. An event can be sent twice if the service stops between the send and the commit;
 * that is why consumers are idempotent.
 */
@Component
class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private final OutboxEventRepository outbox;
    private final KafkaTemplate<String, String> kafka;
    private final MessagingProperties properties;
    private final Clock clock;

    OutboxRelay(OutboxEventRepository outbox, KafkaTemplate<String, String> kafka, MessagingProperties properties,
                Clock clock) {
        this.outbox = outbox;
        this.kafka = kafka;
        this.properties = properties;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${claims.messaging.outbox-poll-interval}")
    @Transactional
    public void publishPending() {
        List<OutboxEvent> pending = outbox.lockNextUnpublished(properties.outboxBatchSize());
        for (OutboxEvent event : pending) {
            send(event);
            event.markPublished(clock.instant());
        }
        if (!pending.isEmpty()) {
            log.debug("Published {} outbox event(s)", pending.size());
        }
    }

    private void send(OutboxEvent event) {
        // Keyed by claim id so all events of one claim land on one partition and stay in order.
        ProducerRecord<String, String> producerRecord =
                new ProducerRecord<>(properties.topic(), event.getAggregateId().toString(), event.getPayload());
        producerRecord.headers().add("eventId", event.getEventId().toString().getBytes(StandardCharsets.UTF_8));
        producerRecord.headers().add("eventType", event.getEventType().getBytes(StandardCharsets.UTF_8));
        try {
            kafka.send(producerRecord).get(properties.sendTimeout().toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while publishing event " + event.getEventId(), e);
        } catch (ExecutionException | TimeoutException e) {
            throw new IllegalStateException("Could not publish event " + event.getEventId() + "; will retry", e);
        }
    }
}
