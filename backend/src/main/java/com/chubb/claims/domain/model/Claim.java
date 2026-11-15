package com.chubb.claims.domain.model;

import com.chubb.claims.domain.event.ClaimStatusChanged;
import com.chubb.claims.domain.exception.BusinessRuleViolationException;
import com.chubb.claims.domain.exception.InvalidStatusTransitionException;
import com.chubb.claims.domain.exception.ResourceNotFoundException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * The claim aggregate. Every status change goes through one of the intent-revealing methods below, which
 * enforce the workflow, append to the status history and raise a {@link ClaimStatusChanged} event.
 * There is deliberately no public setter for the status.
 */
@Entity
@Table(name = "claim")
public class Claim {

    @Id
    private UUID id;

    @Column(name = "claim_number", nullable = false, unique = true)
    private String claimNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "policy_id", nullable = false)
    private Policy policy;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "claimant_id", nullable = false)
    private AppUser claimant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_officer_id")
    private AppUser assignedOfficer;

    @Column(name = "incident_date", nullable = false)
    private LocalDate incidentDate;

    @Column(name = "incident_location")
    private String incidentLocation;

    @Column(nullable = false)
    private String description;

    @Column(name = "claimed_amount", nullable = false)
    private BigDecimal claimedAmount;

    /** The insurer's current estimate of what it still expects to pay on this claim. */
    @Column(name = "reserve_amount", nullable = false)
    private BigDecimal reserveAmount;

    @Column(name = "approved_amount")
    private BigDecimal approvedAmount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClaimStatus status;

    @Column(name = "decision_reason")
    private String decisionReason;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    @OneToMany(mappedBy = "claim", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("changedAt ASC")
    private List<ClaimStatusChange> statusHistory = new ArrayList<>();

    @OneToMany(mappedBy = "claim", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("requestedAt ASC")
    private List<InformationRequest> informationRequests = new ArrayList<>();

    @Transient
    private final List<ClaimStatusChanged> pendingEvents = new ArrayList<>();

    protected Claim() {
        // for JPA
    }

    /** A claimant reports an incident. The new claim starts in SUBMITTED with a reserve capped at the sum insured. */
    public static Claim submit(String claimNumber, Policy policy, AppUser claimant, LocalDate incidentDate,
                               String incidentLocation, String description, BigDecimal claimedAmount, Instant now,
                               LocalDate today) {
        if (!policy.isHeldBy(claimant)) {
            throw new BusinessRuleViolationException("A claim can only be made against your own policy");
        }
        if (incidentDate.isAfter(today)) {
            throw new BusinessRuleViolationException("The incident date cannot be in the future");
        }
        if (!policy.covers(incidentDate)) {
            throw new BusinessRuleViolationException(
                    "The incident date is outside the cover period of policy " + policy.getPolicyNumber());
        }
        if (claimedAmount.signum() <= 0) {
            throw new BusinessRuleViolationException("The claimed amount must be greater than zero");
        }

        Claim claim = new Claim();
        claim.id = UUID.randomUUID();
        claim.claimNumber = claimNumber;
        claim.policy = policy;
        claim.claimant = claimant;
        claim.incidentDate = incidentDate;
        claim.incidentLocation = incidentLocation;
        claim.description = description;
        claim.claimedAmount = claimedAmount;
        claim.currency = policy.getCurrency();
        claim.reserveAmount = claimedAmount.min(policy.getSumInsured());
        claim.status = ClaimStatus.SUBMITTED;
        claim.submittedAt = now;
        claim.updatedAt = now;
        claim.recordChange(null, BigDecimal.ZERO, claimant, null, now);
        return claim;
    }

    /** A staff member takes the claim from the queue and becomes responsible for it. */
    public void pickUp(AppUser officer, Instant now) {
        transitionTo(ClaimStatus.UNDER_REVIEW, reserveAmount, officer, "Picked up for review", now);
        this.assignedOfficer = officer;
    }

    /** Staff ask the claimant a question; the claim now waits on the claimant. */
    public void requestInformation(AppUser officer, String question, Instant now) {
        transitionTo(ClaimStatus.INFO_REQUESTED, reserveAmount, officer, "Information requested", now);
        informationRequests.add(new InformationRequest(this, question, officer, now));
    }

    /** The claimant answers an open question; the claim returns to the officer. */
    public void answerInformationRequest(UUID requestId, AppUser claimant, String response, Instant now) {
        if (status != ClaimStatus.INFO_REQUESTED) {
            throw new InvalidStatusTransitionException(status, ClaimStatus.UNDER_REVIEW);
        }
        InformationRequest request = informationRequests.stream()
                .filter(candidate -> candidate.getId().equals(requestId) && !candidate.isAnswered())
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("No open information request " + requestId));
        request.answer(response, now);
        transitionTo(ClaimStatus.UNDER_REVIEW, reserveAmount, claimant, "Information provided", now);
    }

    /** Approve payment of an amount. The reserve becomes the approved amount. */
    public void approve(AppUser officer, BigDecimal amount, String note, Instant now) {
        requireTransition(ClaimStatus.APPROVED);
        if (amount == null || amount.signum() <= 0) {
            throw new BusinessRuleViolationException("An approved amount greater than zero is required");
        }
        if (amount.compareTo(policy.getSumInsured()) > 0) {
            throw new BusinessRuleViolationException(
                    "The approved amount cannot exceed the sum insured of %s %s"
                            .formatted(currency, policy.getSumInsured().toPlainString()));
        }
        transitionTo(ClaimStatus.APPROVED, amount, officer, note, now);
        this.approvedAmount = amount;
        this.decisionReason = note;
    }

    /** Reject the claim. A reason is mandatory because the claimant will read it. */
    public void reject(AppUser officer, String reason, Instant now) {
        requireTransition(ClaimStatus.REJECTED);
        if (reason == null || reason.isBlank()) {
            throw new BusinessRuleViolationException("A reason is required to reject a claim");
        }
        transitionTo(ClaimStatus.REJECTED, BigDecimal.ZERO, officer, reason, now);
        this.decisionReason = reason;
    }

    /** Record that the approved amount has been paid. Nothing remains outstanding. */
    public void settle(AppUser officer, Instant now) {
        transitionTo(ClaimStatus.SETTLED, BigDecimal.ZERO, officer, "Payment recorded", now);
    }

    /** What the insurer may still have to pay: the reserve while the claim is open, otherwise zero. */
    public BigDecimal outstandingAmount() {
        return status.isOpen() ? reserveAmount : BigDecimal.ZERO;
    }

    public boolean isOwnedBy(UUID userId) {
        return claimant.getId().equals(userId);
    }

    public boolean isAssignedTo(UUID userId) {
        return assignedOfficer != null && assignedOfficer.getId().equals(userId);
    }

    /** Returns the events raised since the last call and clears them. The service publishes them. */
    public List<ClaimStatusChanged> pullEvents() {
        List<ClaimStatusChanged> events = List.copyOf(pendingEvents);
        pendingEvents.clear();
        return events;
    }

    private void requireTransition(ClaimStatus next) {
        if (!status.canTransitionTo(next)) {
            throw new InvalidStatusTransitionException(status, next);
        }
    }

    private void transitionTo(ClaimStatus next, BigDecimal newReserve, AppUser actor, String note, Instant now) {
        requireTransition(next);
        ClaimStatus previousStatus = status;
        BigDecimal previousOutstanding = outstandingAmount();
        this.status = next;
        this.reserveAmount = newReserve;
        this.updatedAt = now;
        recordChange(previousStatus, previousOutstanding, actor, note, now);
    }

    private void recordChange(ClaimStatus previousStatus, BigDecimal previousOutstanding, AppUser actor, String note,
                              Instant now) {
        statusHistory.add(new ClaimStatusChange(this, previousStatus, status, actor, note, now));
        pendingEvents.add(new ClaimStatusChanged(UUID.randomUUID(), now, id, claimNumber, previousStatus, status,
                policy.getCountryCode(), policy.getLineOfBusiness(), currency, previousOutstanding,
                outstandingAmount(), actor.getId()));
    }

    public UUID getId() {
        return id;
    }

    public String getClaimNumber() {
        return claimNumber;
    }

    public Policy getPolicy() {
        return policy;
    }

    public AppUser getClaimant() {
        return claimant;
    }

    public AppUser getAssignedOfficer() {
        return assignedOfficer;
    }

    public LocalDate getIncidentDate() {
        return incidentDate;
    }

    public String getIncidentLocation() {
        return incidentLocation;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getClaimedAmount() {
        return claimedAmount;
    }

    public BigDecimal getReserveAmount() {
        return reserveAmount;
    }

    public BigDecimal getApprovedAmount() {
        return approvedAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public ClaimStatus getStatus() {
        return status;
    }

    public String getDecisionReason() {
        return decisionReason;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<ClaimStatusChange> getStatusHistory() {
        return Collections.unmodifiableList(statusHistory);
    }

    public List<InformationRequest> getInformationRequests() {
        return Collections.unmodifiableList(informationRequests);
    }
}
