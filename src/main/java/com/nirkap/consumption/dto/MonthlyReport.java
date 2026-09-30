package com.nirkap.consumption.dto;

import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;

// byUser is only filled for an organization report; for a user report it is
// null and left out of the JSON.
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MonthlyReport(
        String month,
        long totalTokens,
        Map<String, Long> byService,
        Map<UUID, Long> byUser) {
}
