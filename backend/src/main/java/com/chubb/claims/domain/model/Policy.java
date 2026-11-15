package com.chubb.claims.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** An insurance policy a claim is made against. It carries the market: country and currency. */
@Entity
@Table(name = "policy")
public class Policy {

    @Id
    private UUID id;

    @Column(name = "policy_number", nullable = false, unique = true)
    private String policyNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "holder_id", nullable = false)
    private AppUser holder;

    @Enumerated(EnumType.STRING)
    @Column(name = "line_of_business", nullable = false)
    private LineOfBusiness lineOfBusiness;

    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "sum_insured", nullable = false)
    private BigDecimal sumInsured;

    @Column(name = "cover_start", nullable = false)
    private LocalDate coverStart;

    @Column(name = "cover_end", nullable = false)
    private LocalDate coverEnd;

    protected Policy() {
        // for JPA
    }

    public Policy(UUID id, String policyNumber, AppUser holder, LineOfBusiness lineOfBusiness, String countryCode,
                  String currency, BigDecimal sumInsured, LocalDate coverStart, LocalDate coverEnd) {
        this.id = id;
        this.policyNumber = policyNumber;
        this.holder = holder;
        this.lineOfBusiness = lineOfBusiness;
        this.countryCode = countryCode;
        this.currency = currency;
        this.sumInsured = sumInsured;
        this.coverStart = coverStart;
        this.coverEnd = coverEnd;
    }

    public boolean isHeldBy(AppUser user) {
        return holder.getId().equals(user.getId());
    }

    public boolean covers(LocalDate date) {
        return !date.isBefore(coverStart) && !date.isAfter(coverEnd);
    }

    public UUID getId() {
        return id;
    }

    public String getPolicyNumber() {
        return policyNumber;
    }

    public AppUser getHolder() {
        return holder;
    }

    public LineOfBusiness getLineOfBusiness() {
        return lineOfBusiness;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getSumInsured() {
        return sumInsured;
    }

    public LocalDate getCoverStart() {
        return coverStart;
    }

    public LocalDate getCoverEnd() {
        return coverEnd;
    }
}
