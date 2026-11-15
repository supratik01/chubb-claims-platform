package com.chubb.claims.api.dto;

import com.chubb.claims.domain.model.ClaimStatus;
import com.chubb.claims.domain.model.LineOfBusiness;
import com.chubb.claims.service.view.ClaimSummaryView;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ClaimSummaryResponse(
        UUID id,
        String claimNumber,
        String policyNumber,
        LineOfBusiness lineOfBusiness,
        String countryCode,
        ClaimStatus status,
        BigDecimal claimedAmount,
        String currency,
        LocalDate incidentDate,
        String claimantName,
        UUID assignedOfficerId,
        String assignedOfficerName,
        Instant submittedAt,
        Instant updatedAt) {

    public static ClaimSummaryResponse from(ClaimSummaryView claim) {
        return new ClaimSummaryResponse(claim.id(), claim.claimNumber(), claim.policyNumber(), claim.lineOfBusiness(),
                claim.countryCode(), claim.status(), claim.claimedAmount(), claim.currency(), claim.incidentDate(),
                claim.claimantName(), claim.assignedOfficerId(), claim.assignedOfficerName(), claim.submittedAt(),
                claim.updatedAt());
    }
}
