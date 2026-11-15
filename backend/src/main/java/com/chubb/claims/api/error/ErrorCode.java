package com.chubb.claims.api.error;

/** The stable, machine-readable codes carried in the "code" field of every problem response. */
public enum ErrorCode {
    VALIDATION_FAILED,
    UNAUTHENTICATED,
    INVALID_CREDENTIALS,
    ACCESS_DENIED,
    NOT_FOUND,
    INVALID_STATUS_TRANSITION,
    CONCURRENT_MODIFICATION,
    BUSINESS_RULE_VIOLATED,
    INTERNAL_ERROR
}
