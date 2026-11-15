package com.chubb.claims.service.command;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record SubmitClaimCommand(UUID policyId, LocalDate incidentDate, String incidentLocation, String description,
                                 BigDecimal claimedAmount) {
}
