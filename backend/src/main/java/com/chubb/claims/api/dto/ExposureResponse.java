package com.chubb.claims.api.dto;

import com.chubb.claims.domain.model.LineOfBusiness;
import com.chubb.claims.service.view.ExposureView;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ExposureResponse(List<CurrencyExposure> totals, List<ExposureLine> breakdown) {

    public static ExposureResponse from(ExposureView exposure) {
        return new ExposureResponse(
                exposure.totals().stream()
                        .map(total -> new CurrencyExposure(total.currency(), total.openClaims(),
                                total.outstandingAmount()))
                        .toList(),
                exposure.breakdown().stream()
                        .map(line -> new ExposureLine(line.countryCode(), line.lineOfBusiness(), line.currency(),
                                line.openClaims(), line.outstandingAmount(), line.updatedAt()))
                        .toList());
    }

    public record CurrencyExposure(String currency, long openClaims, BigDecimal outstandingAmount) {
    }

    public record ExposureLine(String countryCode, LineOfBusiness lineOfBusiness, String currency, long openClaims,
                               BigDecimal outstandingAmount, Instant updatedAt) {
    }
}
