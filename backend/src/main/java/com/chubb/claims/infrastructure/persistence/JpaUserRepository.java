package com.chubb.claims.infrastructure.persistence;

import com.chubb.claims.domain.model.AppUser;
import com.chubb.claims.domain.repository.UserRepository;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface JpaUserRepository extends JpaRepository<AppUser, UUID>, UserRepository {
}
