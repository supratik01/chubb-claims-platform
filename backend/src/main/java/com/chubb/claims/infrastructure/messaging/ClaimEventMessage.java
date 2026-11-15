package com.chubb.claims.infrastructure.messaging;

import com.chubb.claims.domain.event.ClaimStatusChanged;
import com.chubb.claims.domain.model.ClaimStatus;
import com.chubb.claims.domain.model.LineOfBusiness;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * The JSON published to Kafka for a {@link ClaimStatusChanged} event (schema version 1).
 * Kept separate from the domain event so the wire format can evolve without touching the domain.
 */
public record ClaimEventMessage(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        UUID claimId,
        String claimNumber,
        ClaimStatus previousStatus,
        ClaimStatus newStatus,
        String countryCode,
        LineOfBusiness lineOfBusiness,
        String currency,
        BigDecimal previousOutstandingAmount,
        BigDecimal newOutstandingAmount,
        UUID changedByUserId) {

    public static final String EVENT_TYPE = "ClaimStatusChanged";

    static ClaimEventMessage from(ClaimStatusChanged event) {
        return new ClaimEventMessage(event.eventId(), EVENT_TYPE, event.occurredAt(), event.claimId(),
                event.claimNumber(), event.previousStatus(), event.newStatus(), event.countryCode(),
                event.lineOfBusiness(), event.currency(), event.previousOutstandingAmount(),
                event.newOutstandingAmount(), event.changedByUserId());
    }

    ClaimStatusChanged toDomain() {
        return new ClaimStatusChanged(eventId, occurredAt, claimId, claimNumber, previousStatus, newStatus,
                countryCode, lineOfBusiness, currency, previousOutstandingAmount, newOutstandingAmount,
                changedByUserId);
    }
}
