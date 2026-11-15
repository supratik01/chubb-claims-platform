package com.chubb.claims.service;

import com.chubb.claims.domain.event.ClaimEventPublisher;
import com.chubb.claims.domain.exception.BusinessRuleViolationException;
import com.chubb.claims.domain.exception.ResourceNotFoundException;
import com.chubb.claims.domain.model.AppUser;
import com.chubb.claims.domain.model.Claim;
import com.chubb.claims.domain.model.Policy;
import com.chubb.claims.domain.repository.ClaimRepository;
import com.chubb.claims.domain.repository.PolicyRepository;
import com.chubb.claims.domain.repository.UserRepository;
import com.chubb.claims.service.command.SubmitClaimCommand;
import com.chubb.claims.service.view.ClaimDetailView;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClaimSubmissionService {

    /**
     * The service has no per-market time zones yet, so "today" is the latest calendar date anywhere
     * (UTC+14). A claimant in Sydney reporting an incident this morning is never told it is in the future.
     */
    private static final ZoneOffset LATEST_ZONE = ZoneOffset.ofHours(14);

    private final ClaimRepository claims;
    private final PolicyRepository policies;
    private final UserRepository users;
    private final ClaimEventPublisher events;
    private final Clock clock;

    public ClaimSubmissionService(ClaimRepository claims, PolicyRepository policies, UserRepository users,
                                  ClaimEventPublisher events, Clock clock) {
        this.claims = claims;
        this.policies = policies;
        this.users = users;
        this.events = events;
        this.clock = clock;
    }

    @Transactional
    public ClaimDetailView submit(Actor actor, SubmitClaimCommand command) {
        AppUser claimant = users.findById(actor.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User " + actor.userId() + " was not found"));
        Policy policy = policies.findById(command.policyId())
                .filter(candidate -> candidate.isHeldBy(claimant))
                .orElseThrow(() -> new BusinessRuleViolationException(
                        "Policy " + command.policyId() + " is not one of your policies"));

        Instant now = clock.instant();
        LocalDate today = now.atOffset(LATEST_ZONE).toLocalDate();
        Claim claim = Claim.submit(nextClaimNumber(now), policy, claimant, command.incidentDate(),
                command.incidentLocation(), command.description(), command.claimedAmount(), now, today);

        claims.save(claim);
        claim.pullEvents().forEach(events::publish);
        return ClaimDetailView.of(claim, false);
    }

    private String nextClaimNumber(Instant now) {
        int year = now.atOffset(ZoneOffset.UTC).getYear();
        return "CLM-%d-%06d".formatted(year, claims.nextClaimSequence());
    }
}
