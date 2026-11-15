package com.chubb.claims.domain.exception;

/** Base type for every expected business failure. The API layer maps each subtype to one HTTP status. */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }
}
