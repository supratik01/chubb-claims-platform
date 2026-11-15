package com.chubb.claims.service;

import com.chubb.claims.domain.event.ClaimStatusChanged;
import com.chubb.claims.domain.model.ExposureKey;
import com.chubb.claims.domain.repository.ExposureSummaryRepository;
import com.chubb.claims.domain.repository.ProcessedEventRepository;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Keeps the exposure read model up to date from claim events.
 *
 * <p>Events are delivered at least once, so the same event may arrive twice. The event identifier is
 * recorded in the same transaction as the summary update; a repeat is recognised and skipped.
 */
@Service
public class ExposureProjectionService {

    private final ExposureSummaryRepository exposure;
    private final ProcessedEventRepository processedEvents;

    public ExposureProjectionService(ExposureSummaryRepository exposure, ProcessedEventRepository processedEvents) {
        this.exposure = exposure;
        this.processedEvents = processedEvents;
    }

    @Transactional
    public void apply(ClaimStatusChanged event) {
        if (!processedEvents.markProcessed(event.eventId(), event.occurredAt())) {
            return;
        }
        long openClaimDelta = (event.isOpen() ? 1 : 0) - (event.wasOpen() ? 1 : 0);
        BigDecimal outstandingDelta = event.newOutstandingAmount().subtract(event.previousOutstandingAmount());
        if (openClaimDelta == 0 && outstandingDelta.signum() == 0) {
            return;
        }
        exposure.applyDelta(
                new ExposureKey(event.countryCode(), event.lineOfBusiness(), event.currency()),
                openClaimDelta, outstandingDelta, event.occurredAt());
    }
}
