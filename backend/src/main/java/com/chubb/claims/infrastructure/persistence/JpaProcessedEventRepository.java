package com.chubb.claims.infrastructure.persistence;

import com.chubb.claims.domain.repository.ProcessedEventRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

interface JpaProcessedEventRepository extends Repository<ProcessedEvent, UUID>, ProcessedEventRepository {

    @Override
    default boolean markProcessed(UUID eventId, Instant at) {
        return insertIfAbsent(eventId, at) == 1;
    }

    /** Relies on the primary key: a second insert of the same event changes nothing and reports zero rows. */
    @Modifying
    @Query(value = """
            INSERT INTO processed_event (event_id, processed_at) VALUES (:eventId, :at)
            ON CONFLICT (event_id) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("eventId") UUID eventId, @Param("at") Instant at);
}
