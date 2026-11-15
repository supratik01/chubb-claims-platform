package com.chubb.claims.service;

import com.chubb.claims.domain.exception.ResourceNotFoundException;
import com.chubb.claims.domain.model.Claim;
import com.chubb.claims.domain.model.ClaimSearchCriteria;
import com.chubb.claims.domain.model.ClaimStatus;
import com.chubb.claims.domain.repository.ClaimRepository;
import com.chubb.claims.service.command.AssignmentFilter;
import com.chubb.claims.service.view.ClaimDetailView;
import com.chubb.claims.service.view.ClaimSummaryView;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClaimQueryService {

    private final ClaimRepository claims;

    public ClaimQueryService(ClaimRepository claims) {
        this.claims = claims;
    }

    /**
     * Claimants always get exactly their own claims, newest first. Staff get the claims matching the
     * assignment filter, oldest first, so the claim that has waited longest is at the top of the queue.
     */
    @Transactional(readOnly = true)
    public Page<ClaimSummaryView> search(Actor actor, AssignmentFilter assignment, Set<ClaimStatus> statuses,
                                         int page, int size) {
        ClaimSearchCriteria criteria;
        Sort sort;
        if (actor.isStaff()) {
            criteria = new ClaimSearchCriteria(
                    null,
                    assignment == AssignmentFilter.MINE ? actor.userId() : null,
                    assignment == AssignmentFilter.UNASSIGNED,
                    statuses);
            sort = Sort.by("submittedAt").ascending();
        } else {
            criteria = new ClaimSearchCriteria(actor.userId(), null, false, statuses);
            sort = Sort.by("submittedAt").descending();
        }
        return claims.search(criteria, PageRequest.of(page, size, sort)).map(ClaimSummaryView::of);
    }

    @Transactional(readOnly = true)
    public ClaimDetailView get(Actor actor, UUID claimId) {
        Claim claim = claims.findById(claimId)
                .orElseThrow(() -> new ResourceNotFoundException("Claim " + claimId + " was not found"));
        ClaimAccessPolicy.requireCanView(actor, claim);
        return ClaimDetailView.of(claim, actor.isStaff());
    }
}
