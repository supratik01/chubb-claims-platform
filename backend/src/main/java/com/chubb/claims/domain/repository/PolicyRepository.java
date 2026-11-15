package com.chubb.claims.domain.repository;

import com.chubb.claims.domain.model.Policy;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PolicyRepository {

    Policy save(Policy policy);

    Optional<Policy> findById(UUID id);

    List<Policy> findByHolderIdOrderByPolicyNumber(UUID holderId);
}
