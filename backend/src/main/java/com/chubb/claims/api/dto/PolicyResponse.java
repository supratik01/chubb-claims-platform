package com.chubb.claims.api.dto;

import com.chubb.claims.domain.model.LineOfBusiness;
import com.chubb.claims.service.view.PolicyView;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PolicyResponse(UUID id, String policyNumber, LineOfBusiness lineOfBusiness, String countryCode,
                             String currency, BigDecimal sumInsured, LocalDate coverStart, LocalDate coverEnd) {

    public static PolicyResponse from(PolicyView policy) {
        return new PolicyResponse(policy.id(), policy.policyNumber(), policy.lineOfBusiness(), policy.countryCode(),
                policy.currency(), policy.sumInsured(), policy.coverStart(), policy.coverEnd());
    }
}
