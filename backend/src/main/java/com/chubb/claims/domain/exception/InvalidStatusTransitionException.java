package com.chubb.claims.domain.exception;

import com.chubb.claims.domain.model.ClaimStatus;

public class InvalidStatusTransitionException extends DomainException {

    public InvalidStatusTransitionException(ClaimStatus from, ClaimStatus to) {
        super("A claim in status %s cannot move to %s".formatted(from, to));
    }
}
