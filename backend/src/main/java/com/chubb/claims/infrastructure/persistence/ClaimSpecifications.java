package com.chubb.claims.infrastructure.persistence;

import com.chubb.claims.domain.model.Claim;
import com.chubb.claims.domain.model.ClaimSearchCriteria;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

/** Translates {@link ClaimSearchCriteria} into a JPA criteria query, adding only the filters that are set. */
final class ClaimSpecifications {

    private ClaimSpecifications() {
    }

    static Specification<Claim> matching(ClaimSearchCriteria criteria) {
        return (claim, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (criteria.claimantId() != null) {
                predicates.add(builder.equal(claim.get("claimant").get("id"), criteria.claimantId()));
            }
            if (criteria.assignedOfficerId() != null) {
                predicates.add(builder.equal(claim.get("assignedOfficer").get("id"), criteria.assignedOfficerId()));
            }
            if (criteria.unassignedOnly()) {
                predicates.add(builder.isNull(claim.get("assignedOfficer")));
            }
            if (!criteria.statuses().isEmpty()) {
                predicates.add(claim.get("status").in(criteria.statuses()));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
