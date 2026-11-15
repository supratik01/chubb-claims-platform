package com.chubb.claims.api.dto;

import com.chubb.claims.domain.model.ClaimStatus;
import com.chubb.claims.service.view.ClaimDetailView;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** The contract defines ClaimDetail as ClaimSummary plus more fields, so the summary is written inline. */
public record ClaimDetailResponse(
        @JsonUnwrapped ClaimSummaryResponse summary,
        String description,
        String incidentLocation,
        BigDecimal sumInsured,
        BigDecimal reserveAmount,
        BigDecimal approvedAmount,
        String decisionReason,
        List<StatusHistoryEntry> statusHistory,
        List<InformationRequestEntry> informationRequests) {

    public static ClaimDetailResponse from(ClaimDetailView claim) {
        return new ClaimDetailResponse(
                ClaimSummaryResponse.from(claim.summary()),
                claim.description(),
                claim.incidentLocation(),
                claim.sumInsured(),
                claim.reserveAmount(),
                claim.approvedAmount(),
                claim.decisionReason(),
                claim.statusHistory().stream()
                        .map(change -> new StatusHistoryEntry(change.fromStatus(), change.toStatus(),
                                change.changedByName(), change.note(), change.changedAt()))
                        .toList(),
                claim.informationRequests().stream()
                        .map(request -> new InformationRequestEntry(request.id(), request.question(),
                                request.requestedByName(), request.requestedAt(), request.response(),
                                request.respondedAt()))
                        .toList());
    }

    public record StatusHistoryEntry(ClaimStatus fromStatus, ClaimStatus toStatus, String changedByName, String note,
                                     Instant changedAt) {
    }

    public record InformationRequestEntry(UUID id, String question, String requestedByName, Instant requestedAt,
                                          String response, Instant respondedAt) {
    }
}
