package com.chubb.claims.api.dto;

import com.chubb.claims.service.command.SubmitClaimCommand;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record SubmitClaimRequest(
        @NotNull UUID policyId,
        @NotNull LocalDate incidentDate,
        @Size(max = 200) String incidentLocation,
        @NotBlank @Size(min = 10, max = 2000) String description,
        @NotNull @Positive @Digits(integer = 12, fraction = 2) BigDecimal claimedAmount) {

    public SubmitClaimCommand toCommand() {
        return new SubmitClaimCommand(policyId, incidentDate, incidentLocation, description, claimedAmount);
    }
}
