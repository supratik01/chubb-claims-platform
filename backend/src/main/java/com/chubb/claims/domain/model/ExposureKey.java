package com.chubb.claims.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.io.Serializable;

/** Exposure is tracked per market, line of business and currency. Amounts in different currencies are never added. */
@Embeddable
public record ExposureKey(
        @Column(name = "country_code", length = 2) String countryCode,
        @Enumerated(EnumType.STRING) @Column(name = "line_of_business") LineOfBusiness lineOfBusiness,
        @Column(name = "currency", length = 3) String currency) implements Serializable {
}
