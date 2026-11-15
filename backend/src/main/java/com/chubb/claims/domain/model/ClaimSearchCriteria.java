package com.chubb.claims.domain.model;

import java.util.Set;
import java.util.UUID;

/**
 * Filters for listing claims. A null field means "do not filter on this".
 *
 * @param claimantId       only claims submitted by this user
 * @param assignedOfficerId only claims assigned to this user
 * @param unassignedOnly   only claims nobody has picked up
 * @param statuses         only claims in one of these statuses; empty means any
 */
public record ClaimSearchCriteria(UUID claimantId, UUID assignedOfficerId, boolean unassignedOnly,
                                  Set<ClaimStatus> statuses) {

    public ClaimSearchCriteria {
        statuses = statuses == null ? Set.of() : Set.copyOf(statuses);
    }
}
