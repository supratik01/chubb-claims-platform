package com.chubb.claims.api.dto;

import com.chubb.claims.domain.model.Role;
import com.chubb.claims.service.view.SessionView;
import java.util.UUID;

public record LoginResponse(String accessToken, String tokenType, long expiresInSeconds, UserSummary user) {

    public static LoginResponse from(SessionView session) {
        return new LoginResponse(session.accessToken(), "Bearer", session.expiresInSeconds(),
                new UserSummary(session.userId(), session.displayName(), session.role()));
    }

    public record UserSummary(UUID id, String displayName, Role role) {
    }
}
