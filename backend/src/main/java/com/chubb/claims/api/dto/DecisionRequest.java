package com.chubb.claims.api.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record DecisionRequest(
        @NotNull Outcome outcome,
        @Digits(integer = 12, fraction = 2) BigDecimal approvedAmount,
        @Size(max = 1000) String reason) {

    public enum Outcome {
        APPROVED,
        REJECTED
    }
}
