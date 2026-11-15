package com.chubb.claims.service.port;

import com.chubb.claims.domain.model.AppUser;

/** Port for issuing the access token a signed-in user presents on later requests. */
public interface AccessTokenIssuer {

    IssuedToken issueFor(AppUser user);

    record IssuedToken(String value, long expiresInSeconds) {
    }
}
