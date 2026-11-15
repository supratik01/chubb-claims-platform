package com.chubb.claims.domain.model;

public enum Role {
    CLAIMANT,
    CLAIMS_OFFICER,
    CLAIMS_MANAGER;

    public boolean isStaff() {
        return this == CLAIMS_OFFICER || this == CLAIMS_MANAGER;
    }
}
