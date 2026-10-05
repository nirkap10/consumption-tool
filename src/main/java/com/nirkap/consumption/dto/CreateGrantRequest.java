package com.nirkap.consumption.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateGrantRequest(
        @NotBlank @Size(max = 255) String eventId,
        @NotNull UUID userId,
        @NotNull @Positive Long amount,
        @Size(max = 255) String reason) {
}
