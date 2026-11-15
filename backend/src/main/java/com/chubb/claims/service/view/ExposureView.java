package com.chubb.claims.service.view;

import com.chubb.claims.domain.model.LineOfBusiness;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ExposureView(List<CurrencyTotal> totals, List<ExposureLine> breakdown) {

    public record CurrencyTotal(String currency, long openClaims, BigDecimal outstandingAmount) {
    }

    public record ExposureLine(String countryCode, LineOfBusiness lineOfBusiness, String currency, long openClaims,
                               BigDecimal outstandingAmount, Instant updatedAt) {
    }
}
