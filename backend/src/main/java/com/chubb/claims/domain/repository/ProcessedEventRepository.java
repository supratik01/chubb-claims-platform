package com.chubb.claims.domain.repository;

import java.time.Instant;
import java.util.UUID;

/** Remembers which events a consumer has already applied, so that redelivery has no effect. */
public interface ProcessedEventRepository {

    /**
     * Records the event as processed.
     *
     * @return true if this is the first time the event is seen, false if it was already processed
     */
    boolean markProcessed(UUID eventId, Instant at);
}
