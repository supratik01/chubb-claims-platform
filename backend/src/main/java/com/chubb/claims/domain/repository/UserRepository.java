package com.chubb.claims.domain.repository;

import com.chubb.claims.domain.model.AppUser;
import com.chubb.claims.domain.model.Role;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {

    AppUser save(AppUser user);

    Optional<AppUser> findById(UUID id);

    Optional<AppUser> findByUsername(String username);

    List<AppUser> findByRoleOrderByDisplayName(Role role);
}
