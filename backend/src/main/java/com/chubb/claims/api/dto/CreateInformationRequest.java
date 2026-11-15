package com.chubb.claims.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateInformationRequest(@NotBlank @Size(min = 5, max = 1000) String question) {
}
