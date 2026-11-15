package com.chubb.claims.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** A question from staff to the claimant, and the claimant's answer once given. Created only by {@link Claim}. */
@Entity
@Table(name = "information_request")
public class InformationRequest {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "claim_id", nullable = false)
    private Claim claim;

    @Column(nullable = false)
    private String question;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requested_by", nullable = false)
    private AppUser requestedBy;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    private String response;

    @Column(name = "responded_at")
    private Instant respondedAt;

    protected InformationRequest() {
        // for JPA
    }

    InformationRequest(Claim claim, String question, AppUser requestedBy, Instant requestedAt) {
        this.id = UUID.randomUUID();
        this.claim = claim;
        this.question = question;
        this.requestedBy = requestedBy;
        this.requestedAt = requestedAt;
    }

    void answer(String response, Instant respondedAt) {
        this.response = response;
        this.respondedAt = respondedAt;
    }

    public boolean isAnswered() {
        return respondedAt != null;
    }

    public UUID getId() {
        return id;
    }

    public String getQuestion() {
        return question;
    }

    public AppUser getRequestedBy() {
        return requestedBy;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public String getResponse() {
        return response;
    }

    public Instant getRespondedAt() {
        return respondedAt;
    }
}
