package com.chubb.claims.service;

import static com.chubb.claims.support.Fixtures.NOW;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chubb.claims.domain.event.ClaimStatusChanged;
import com.chubb.claims.domain.model.ClaimStatus;
import com.chubb.claims.domain.model.ExposureKey;
import com.chubb.claims.domain.model.LineOfBusiness;
import com.chubb.claims.domain.repository.ExposureSummaryRepository;
import com.chubb.claims.domain.repository.ProcessedEventRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ExposureProjectionServiceTest {

    private static final ExposureKey SG_MOTOR = new ExposureKey("SG", LineOfBusiness.MOTOR, "SGD");

    @Mock
    private ExposureSummaryRepository exposure;
    @Mock
    private ProcessedEventRepository processedEvents;

    private ExposureProjectionService projection;

    @BeforeEach
    void setUp() {
        projection = new ExposureProjectionService(exposure, processedEvents);
        when(processedEvents.markProcessed(any(), any())).thenReturn(true);
    }

    @Test
    void aNewClaimAddsOneOpenClaimAndItsReserve() {
        projection.apply(event(null, ClaimStatus.SUBMITTED, "0", "4200.00"));

        verify(exposure).applyDelta(SG_MOTOR, 1, new BigDecimal("4200.00"), NOW);
    }

    @Test
    void anApprovalChangesTheOutstandingAmountButNotTheOpenCount() {
        projection.apply(event(ClaimStatus.UNDER_REVIEW, ClaimStatus.APPROVED, "4200.00", "3800.00"));

        verify(exposure).applyDelta(SG_MOTOR, 0, new BigDecimal("-400.00"), NOW);
    }

    @Test
    void aSettlementRemovesTheClaimAndWhatWasOutstanding() {
        projection.apply(event(ClaimStatus.APPROVED, ClaimStatus.SETTLED, "3800.00", "0"));

        verify(exposure).applyDelta(SG_MOTOR, -1, new BigDecimal("-3800.00"), NOW);
    }

    @Test
    void aRejectionRemovesTheClaimAndWhatWasOutstanding() {
        projection.apply(event(ClaimStatus.UNDER_REVIEW, ClaimStatus.REJECTED, "4200.00", "0"));

        verify(exposure).applyDelta(SG_MOTOR, -1, new BigDecimal("-4200.00"), NOW);
    }

    @Test
    void aChangeThatAltersNeitherCountNorAmountWritesNothing() {
        projection.apply(event(ClaimStatus.UNDER_REVIEW, ClaimStatus.INFO_REQUESTED, "4200.00", "4200.00"));

        verify(exposure, never()).applyDelta(any(), anyLong(), any(), any());
    }

    @Test
    void anEventThatWasAlreadyProcessedIsIgnored() {
        ClaimStatusChanged event = event(null, ClaimStatus.SUBMITTED, "0", "4200.00");
        when(processedEvents.markProcessed(eq(event.eventId()), any())).thenReturn(false);

        projection.apply(event);

        verify(exposure, never()).applyDelta(any(), anyLong(), any(), any());
    }

    private static ClaimStatusChanged event(ClaimStatus from, ClaimStatus to, String before, String after) {
        return new ClaimStatusChanged(UUID.randomUUID(), NOW, UUID.randomUUID(), "CLM-2026-000001", from, to, "SG",
                LineOfBusiness.MOTOR, "SGD", new BigDecimal(before), new BigDecimal(after), UUID.randomUUID());
    }
}
