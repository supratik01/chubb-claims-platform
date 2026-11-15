package com.chubb.claims.api.dto;

import com.chubb.claims.service.view.WorkloadView;
import java.util.List;
import java.util.UUID;

public record WorkloadResponse(long unassignedCount, List<OfficerWorkload> officers) {

    public static WorkloadResponse from(WorkloadView workload) {
        return new WorkloadResponse(workload.unassignedCount(), workload.officers().stream()
                .map(officer -> new OfficerWorkload(officer.officerId(), officer.officerName(), officer.underReview(),
                        officer.awaitingInformation(), officer.awaitingSettlement(), officer.closed()))
                .toList());
    }

    public record OfficerWorkload(UUID officerId, String officerName, long underReview, long awaitingInformation,
                                  long awaitingSettlement, long closed) {
    }
}
