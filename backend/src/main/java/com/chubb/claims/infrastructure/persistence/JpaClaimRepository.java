package com.chubb.claims.infrastructure.persistence;

import com.chubb.claims.domain.model.Claim;
import com.chubb.claims.domain.model.ClaimSearchCriteria;
import com.chubb.claims.domain.model.OfficerStatusCount;
import com.chubb.claims.domain.repository.ClaimRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

/**
 * Spring Data implementation of the {@link ClaimRepository} port. Spring Data generates the class; the
 * service layer only ever sees the port.
 */
interface JpaClaimRepository
        extends JpaRepository<Claim, UUID>, JpaSpecificationExecutor<Claim>, ClaimRepository {

    @Override
    default Page<Claim> search(ClaimSearchCriteria criteria, Pageable pageable) {
        return findAll(ClaimSpecifications.matching(criteria), pageable);
    }

    /** List screens show the policy, claimant and officer, so fetch them with the page instead of one by one. */
    @Override
    @EntityGraph(attributePaths = {"policy", "claimant", "assignedOfficer"})
    Page<Claim> findAll(Specification<Claim> specification, Pageable pageable);

    @Override
    @Query(value = "SELECT nextval('claim_number_seq')", nativeQuery = true)
    long nextClaimSequence();

    @Override
    @Query("""
            SELECT new com.chubb.claims.domain.model.OfficerStatusCount(o.id, o.displayName, c.status, COUNT(c))
            FROM Claim c JOIN c.assignedOfficer o
            GROUP BY o.id, o.displayName, c.status
            """)
    List<OfficerStatusCount> countAssignedClaimsByOfficerAndStatus();
}
