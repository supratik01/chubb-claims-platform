package com.chubb.claims.domain.model;

import java.util.UUID;

/** How many claims one officer holds in one status. The raw material of the workload view. */
public record OfficerStatusCount(UUID officerId, String officerName, ClaimStatus status, long claimCount) {
}
