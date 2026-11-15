package com.chubb.claims.domain.repository;

import com.chubb.claims.domain.model.Claim;
import com.chubb.claims.domain.model.ClaimSearchCriteria;
import com.chubb.claims.domain.model.ClaimStatus;
import com.chubb.claims.domain.model.OfficerStatusCount;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** Port for storing and finding claims. Implemented in the infrastructure layer. */
public interface ClaimRepository {

    Claim save(Claim claim);

    Optional<Claim> findById(UUID id);

    Page<Claim> search(ClaimSearchCriteria criteria, Pageable pageable);

    /** The next value of the claim number sequence; unique across concurrent callers. */
    long nextClaimSequence();

    List<OfficerStatusCount> countAssignedClaimsByOfficerAndStatus();

    long countByStatus(ClaimStatus status);
}
