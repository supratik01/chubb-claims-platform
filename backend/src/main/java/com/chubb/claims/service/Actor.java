package com.chubb.claims.service;

import com.chubb.claims.domain.model.Role;
import java.util.UUID;

/**
 * The authenticated caller of a use case. Services receive it as a parameter instead of reading a
 * security context, which keeps them independent of the web layer and easy to test.
 */
public record Actor(UUID userId, Role role) {

    public boolean isStaff() {
        return role.isStaff();
    }

    public boolean isManager() {
        return role == Role.CLAIMS_MANAGER;
    }
}
