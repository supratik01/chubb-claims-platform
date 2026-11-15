package com.chubb.claims.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InformationResponse(@NotBlank @Size(max = 2000) String response) {
}
