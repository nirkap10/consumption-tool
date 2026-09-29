package com.nirkap.consumption.dto;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record RecordUsageRequest(
        @NotNull UUID userId,
        @NotBlank @Size(max = 255) String serviceName,
        @NotNull @Positive Long tokensUsed,
        Instant occurredAt) {
}
