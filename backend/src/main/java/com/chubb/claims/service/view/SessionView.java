package com.chubb.claims.service.view;

import com.chubb.claims.domain.model.Role;
import java.util.UUID;

/** The result of a successful login. */
public record SessionView(String accessToken, long expiresInSeconds, UUID userId, String displayName, Role role) {
}
