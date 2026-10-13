package com.nirkap.consumption.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record CreateUserRequest(
        @NotBlank String organizationId,
        @NotNull @PositiveOrZero Long monthlyAllowance) {
}
