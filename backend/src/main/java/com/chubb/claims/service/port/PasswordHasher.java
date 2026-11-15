package com.chubb.claims.service.port;

/** Port for password hashing, so the service layer does not depend on a security library. */
public interface PasswordHasher {

    String hash(String rawPassword);

    boolean matches(String rawPassword, String passwordHash);
}
