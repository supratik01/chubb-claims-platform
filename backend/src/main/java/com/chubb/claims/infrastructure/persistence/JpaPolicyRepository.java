package com.chubb.claims.infrastructure.persistence;

import com.chubb.claims.domain.model.Policy;
import com.chubb.claims.domain.repository.PolicyRepository;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface JpaPolicyRepository extends JpaRepository<Policy, UUID>, PolicyRepository {
}
