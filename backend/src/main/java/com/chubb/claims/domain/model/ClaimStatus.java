package com.chubb.claims.domain.model;

import java.util.Set;

/**
 * The claim lifecycle. The allowed moves are declared here and nowhere else.
 *
 * <pre>
 * SUBMITTED -> UNDER_REVIEW <-> INFO_REQUESTED
 *              UNDER_REVIEW -> APPROVED -> SETTLED
 *              UNDER_REVIEW -> REJECTED
 * </pre>
 */
public enum ClaimStatus {
    SUBMITTED,
    UNDER_REVIEW,
    INFO_REQUESTED,
    APPROVED,
    REJECTED,
    SETTLED;

    public Set<ClaimStatus> allowedNext() {
        return switch (this) {
            case SUBMITTED -> Set.of(UNDER_REVIEW);
            case UNDER_REVIEW -> Set.of(INFO_REQUESTED, APPROVED, REJECTED);
            case INFO_REQUESTED -> Set.of(UNDER_REVIEW);
            case APPROVED -> Set.of(SETTLED);
            case REJECTED, SETTLED -> Set.of();
        };
    }

    public boolean canTransitionTo(ClaimStatus next) {
        return allowedNext().contains(next);
    }

    /** An open claim is one the insurer may still have to pay. */
    public boolean isOpen() {
        return this != REJECTED && this != SETTLED;
    }
}
