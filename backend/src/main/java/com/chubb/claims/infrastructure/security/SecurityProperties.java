package com.chubb.claims.infrastructure.security;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param jwtSecret the HMAC key used to sign and verify access tokens; at least 32 characters
 * @param tokenTtl  how long an access token is valid
 */
@ConfigurationProperties("claims.security")
public record SecurityProperties(String jwtSecret, Duration tokenTtl) {

    public SecurityProperties {
        if (jwtSecret == null || jwtSecret.length() < 32) {
            throw new IllegalArgumentException("claims.security.jwt-secret must be at least 32 characters");
        }
    }
}
