package com.chubb.claims.domain.event;

import com.chubb.claims.domain.model.ClaimStatus;
import com.chubb.claims.domain.model.LineOfBusiness;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Raised every time a claim changes status, including its creation (previousStatus is null).
 *
 * <p>It carries the outstanding amount before and after the change so a consumer can keep a running total
 * without calling the claims service back.
 */
public record ClaimStatusChanged(
        UUID eventId,
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

    public boolean wasOpen() {
        return previousStatus != null && previousStatus.isOpen();
    }

    public boolean isOpen() {
        return newStatus.isOpen();
    }
}
