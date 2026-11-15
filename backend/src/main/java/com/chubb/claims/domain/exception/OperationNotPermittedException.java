package com.chubb.claims.domain.exception;

public class OperationNotPermittedException extends DomainException {

    public OperationNotPermittedException(String message) {
        super(message);
    }
}
