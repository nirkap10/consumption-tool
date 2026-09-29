package com.nirkap.consumption.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record CreateUserRequest(
        @NotNull UUID organizationId,
        @NotNull @PositiveOrZero Long monthlyAllowance) {
}
