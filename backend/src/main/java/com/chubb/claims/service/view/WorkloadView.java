package com.chubb.claims.service.view;

import java.util.List;
import java.util.UUID;

public record WorkloadView(long unassignedCount, List<OfficerWorkloadView> officers) {

    /**
     * @param underReview         claims waiting on the officer
     * @param awaitingInformation claims waiting on the claimant
     * @param awaitingSettlement  approved claims whose payment is not yet recorded
     * @param closed              rejected or settled claims
     */
    public record OfficerWorkloadView(UUID officerId, String officerName, long underReview, long awaitingInformation,
                                      long awaitingSettlement, long closed) {
    }
}
