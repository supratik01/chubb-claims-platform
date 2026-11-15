package com.chubb.claims.infrastructure.messaging;

import com.chubb.claims.domain.event.ClaimEventPublisher;
import com.chubb.claims.domain.event.ClaimStatusChanged;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/**
 * Implements event publishing by writing to the outbox table. Because the write joins the caller's
 * transaction, the event is stored if and only if the claim change is committed. {@link OutboxRelay}
 * sends it to Kafka afterwards.
 */
@Component
class OutboxClaimEventPublisher implements ClaimEventPublisher {

    private final OutboxEventRepository outbox;
    private final ObjectMapper objectMapper;

    OutboxClaimEventPublisher(OutboxEventRepository outbox, ObjectMapper objectMapper) {
        this.outbox = outbox;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void publish(ClaimStatusChanged event) {
        String payload = objectMapper.writeValueAsString(ClaimEventMessage.from(event));
        outbox.save(new OutboxEvent(event.eventId(), event.claimId(), ClaimEventMessage.EVENT_TYPE, payload,
                event.occurredAt()));
    }
}
