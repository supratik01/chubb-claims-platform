package com.chubb.claims.service;

import com.chubb.claims.domain.event.ClaimEventPublisher;
import com.chubb.claims.domain.exception.OperationNotPermittedException;
import com.chubb.claims.domain.exception.ResourceNotFoundException;
import com.chubb.claims.domain.model.AppUser;
import com.chubb.claims.domain.model.Claim;
import com.chubb.claims.domain.repository.ClaimRepository;
import com.chubb.claims.domain.repository.UserRepository;
import com.chubb.claims.service.view.ClaimDetailView;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.UUID;
import java.util.function.BiConsumer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The use cases that move a claim through its workflow. Each one checks who is acting, delegates the rule to
 * the {@link Claim} aggregate, and publishes the resulting events in the same transaction.
 */
@Service
public class ClaimWorkflowService {

    private final ClaimRepository claims;
    private final UserRepository users;
    private final ClaimEventPublisher events;
    private final Clock clock;

    public ClaimWorkflowService(ClaimRepository claims, UserRepository users, ClaimEventPublisher events,
                                Clock clock) {
        this.claims = claims;
        this.users = users;
        this.events = events;
        this.clock = clock;
    }

    @Transactional
    public ClaimDetailView pickUp(Actor actor, UUID claimId) {
        if (!actor.isStaff()) {
            throw new OperationNotPermittedException("Only claims staff can pick up a claim");
        }
        return change(actor, claimId, (claim, officer) -> claim.pickUp(officer, clock.instant()));
    }

    @Transactional
    public ClaimDetailView requestInformation(Actor actor, UUID claimId, String question) {
        return change(actor, claimId, (claim, officer) -> {
            ClaimAccessPolicy.requireCanWorkOn(actor, claim);
            claim.requestInformation(officer, question, clock.instant());
        });
    }

    @Transactional
    public ClaimDetailView answerInformationRequest(Actor actor, UUID claimId, UUID requestId, String response) {
        return change(actor, claimId, (claim, claimant) -> {
            if (!claim.isOwnedBy(actor.userId())) {
                throw new OperationNotPermittedException("Only the claimant can answer an information request");
            }
            claim.answerInformationRequest(requestId, claimant, response, clock.instant());
        });
    }

    @Transactional
    public ClaimDetailView approve(Actor actor, UUID claimId, BigDecimal amount, String note) {
        return change(actor, claimId, (claim, officer) -> {
            ClaimAccessPolicy.requireCanWorkOn(actor, claim);
            claim.approve(officer, amount, note, clock.instant());
        });
    }

    @Transactional
    public ClaimDetailView reject(Actor actor, UUID claimId, String reason) {
        return change(actor, claimId, (claim, officer) -> {
            ClaimAccessPolicy.requireCanWorkOn(actor, claim);
            claim.reject(officer, reason, clock.instant());
        });
    }

    @Transactional
    public ClaimDetailView settle(Actor actor, UUID claimId) {
        return change(actor, claimId, (claim, officer) -> {
            ClaimAccessPolicy.requireCanWorkOn(actor, claim);
            claim.settle(officer, clock.instant());
        });
    }

    /** Load, check visibility, apply the change, save and publish. The shape every workflow use case shares. */
    private ClaimDetailView change(Actor actor, UUID claimId, BiConsumer<Claim, AppUser> action) {
        Claim claim = claims.findById(claimId)
                .orElseThrow(() -> new ResourceNotFoundException("Claim " + claimId + " was not found"));
        ClaimAccessPolicy.requireCanView(actor, claim);
        AppUser user = users.findById(actor.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User " + actor.userId() + " was not found"));

        action.accept(claim, user);

        claims.save(claim);
        claim.pullEvents().forEach(events::publish);
        return ClaimDetailView.of(claim, actor.isStaff());
    }
}
