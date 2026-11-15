package com.chubb.claims.service;

import com.chubb.claims.domain.exception.InvalidCredentialsException;
import com.chubb.claims.domain.model.AppUser;
import com.chubb.claims.domain.repository.UserRepository;
import com.chubb.claims.service.port.AccessTokenIssuer;
import com.chubb.claims.service.port.PasswordHasher;
import com.chubb.claims.service.view.SessionView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthenticationService {

    private final UserRepository users;
    private final PasswordHasher passwordHasher;
    private final AccessTokenIssuer tokenIssuer;

    public AuthenticationService(UserRepository users, PasswordHasher passwordHasher, AccessTokenIssuer tokenIssuer) {
        this.users = users;
        this.passwordHasher = passwordHasher;
        this.tokenIssuer = tokenIssuer;
    }

    /** An unknown username and a wrong password fail identically, so usernames cannot be enumerated. */
    @Transactional(readOnly = true)
    public SessionView login(String username, String password) {
        AppUser user = users.findByUsername(username)
                .filter(candidate -> passwordHasher.matches(password, candidate.getPasswordHash()))
                .orElseThrow(InvalidCredentialsException::new);
        AccessTokenIssuer.IssuedToken token = tokenIssuer.issueFor(user);
        return new SessionView(token.value(), token.expiresInSeconds(), user.getId(), user.getDisplayName(),
                user.getRole());
    }
}
