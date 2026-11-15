package com.chubb.claims.service.view;

import com.chubb.claims.domain.model.Claim;
import com.chubb.claims.domain.model.ClaimStatus;
import com.chubb.claims.domain.model.ClaimStatusChange;
import com.chubb.claims.domain.model.InformationRequest;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Immutable snapshot of one claim with its history and questions.
 * The sum insured and reserve are internal figures and are only filled in for staff.
 */
public record ClaimDetailView(
        ClaimSummaryView summary,
        String description,
        String incidentLocation,
        BigDecimal sumInsured,
        BigDecimal reserveAmount,
        BigDecimal approvedAmount,
        String decisionReason,
        List<StatusChangeView> statusHistory,
        List<InformationRequestView> informationRequests) {

    public static ClaimDetailView of(Claim claim, boolean includeInternalFigures) {
        return new ClaimDetailView(
                ClaimSummaryView.of(claim),
                claim.getDescription(),
                claim.getIncidentLocation(),
                includeInternalFigures ? claim.getPolicy().getSumInsured() : null,
                includeInternalFigures ? claim.getReserveAmount() : null,
                claim.getApprovedAmount(),
                claim.getDecisionReason(),
                claim.getStatusHistory().stream().map(StatusChangeView::of).toList(),
                claim.getInformationRequests().stream().map(InformationRequestView::of).toList());
    }

    public record StatusChangeView(ClaimStatus fromStatus, ClaimStatus toStatus, String changedByName, String note,
                                   Instant changedAt) {

        static StatusChangeView of(ClaimStatusChange change) {
            return new StatusChangeView(change.getFromStatus(), change.getToStatus(),
                    change.getChangedBy().getDisplayName(), change.getNote(), change.getChangedAt());
        }
    }

    public record InformationRequestView(UUID id, String question, String requestedByName, Instant requestedAt,
                                         String response, Instant respondedAt) {

        static InformationRequestView of(InformationRequest request) {
            return new InformationRequestView(request.getId(), request.getQuestion(),
                    request.getRequestedBy().getDisplayName(), request.getRequestedAt(), request.getResponse(),
                    request.getRespondedAt());
        }
    }
}
