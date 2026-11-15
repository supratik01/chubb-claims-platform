package com.chubb.claims.service.view;

import com.chubb.claims.domain.model.Claim;
import com.chubb.claims.domain.model.ClaimStatus;
import com.chubb.claims.domain.model.LineOfBusiness;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Immutable snapshot of a claim for list screens, built inside the transaction that loaded it. */
public record ClaimSummaryView(
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

    public static ClaimSummaryView of(Claim claim) {
        var officer = claim.getAssignedOfficer();
        return new ClaimSummaryView(
                claim.getId(),
                claim.getClaimNumber(),
                claim.getPolicy().getPolicyNumber(),
                claim.getPolicy().getLineOfBusiness(),
                claim.getPolicy().getCountryCode(),
                claim.getStatus(),
                claim.getClaimedAmount(),
                claim.getCurrency(),
                claim.getIncidentDate(),
                claim.getClaimant().getDisplayName(),
                officer == null ? null : officer.getId(),
                officer == null ? null : officer.getDisplayName(),
                claim.getSubmittedAt(),
                claim.getUpdatedAt());
    }
}
