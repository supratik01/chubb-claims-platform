package com.chubb.claims.service.view;

import com.chubb.claims.domain.model.LineOfBusiness;
import com.chubb.claims.domain.model.Policy;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PolicyView(UUID id, String policyNumber, LineOfBusiness lineOfBusiness, String countryCode,
                         String currency, BigDecimal sumInsured, LocalDate coverStart, LocalDate coverEnd) {

    public static PolicyView of(Policy policy) {
        return new PolicyView(policy.getId(), policy.getPolicyNumber(), policy.getLineOfBusiness(),
                policy.getCountryCode(), policy.getCurrency(), policy.getSumInsured(), policy.getCoverStart(),
                policy.getCoverEnd());
    }
}
