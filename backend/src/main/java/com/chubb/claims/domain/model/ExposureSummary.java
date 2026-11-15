package com.chubb.claims.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Read model for managers: how many claims are open and how much is outstanding for one {@link ExposureKey}.
 * Rows are written only by the exposure projection, from claim events; the application never edits them directly.
 */
@Entity
@Table(name = "exposure_summary")
public class ExposureSummary {

    @EmbeddedId
    private ExposureKey key;

    @Column(name = "open_claim_count", nullable = false)
    private long openClaimCount;

    @Column(name = "outstanding_amount", nullable = false)
    private BigDecimal outstandingAmount;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ExposureSummary() {
        // for JPA
    }

    public ExposureKey getKey() {
        return key;
    }

    public long getOpenClaimCount() {
        return openClaimCount;
    }

    public BigDecimal getOutstandingAmount() {
        return outstandingAmount;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
