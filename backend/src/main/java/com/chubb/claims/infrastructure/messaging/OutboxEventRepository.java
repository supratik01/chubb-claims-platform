package com.chubb.claims.infrastructure.messaging;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    /**
     * The oldest unpublished events, locked for this transaction. SKIP LOCKED lets several instances of the
     * service run a relay at once without publishing the same row twice.
     */
    @Query(value = """
            SELECT * FROM outbox_event
            WHERE published_at IS NULL
            ORDER BY id
            LIMIT :batchSize
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<OutboxEvent> lockNextUnpublished(@Param("batchSize") int batchSize);

    long countByPublishedAtIsNull();
}
