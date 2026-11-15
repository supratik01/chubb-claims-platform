package com.chubb.claims.domain.exception;

public class InvalidCredentialsException extends DomainException {

    public InvalidCredentialsException() {
        super("The username or password is incorrect");
    }
}
