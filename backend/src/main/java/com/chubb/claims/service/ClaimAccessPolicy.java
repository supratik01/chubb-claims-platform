package com.chubb.claims.service;

import com.chubb.claims.domain.exception.OperationNotPermittedException;
import com.chubb.claims.domain.exception.ResourceNotFoundException;
import com.chubb.claims.domain.model.Claim;

/** The rules for who may see and who may work on a particular claim, in one place. */
final class ClaimAccessPolicy {

    private ClaimAccessPolicy() {
    }

    /**
     * Staff may see every claim; a claimant only their own. A claimant asking for someone else's claim is
     * told it does not exist, so that claim identifiers cannot be probed.
     */
    static void requireCanView(Actor actor, Claim claim) {
        if (!actor.isStaff() && !claim.isOwnedBy(actor.userId())) {
            throw new ResourceNotFoundException("Claim " + claim.getId() + " was not found");
        }
    }

    /** Only the officer the claim is assigned to, or a manager, may progress it. */
    static void requireCanWorkOn(Actor actor, Claim claim) {
        if (!actor.isManager() && !claim.isAssignedTo(actor.userId())) {
            throw new OperationNotPermittedException("Only the assigned officer or a manager can act on this claim");
        }
    }
}
