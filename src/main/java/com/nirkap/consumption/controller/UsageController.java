package com.nirkap.consumption.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.nirkap.consumption.dto.RecordUsageRequest;
import com.nirkap.consumption.service.UsageService;

import jakarta.validation.Valid;

@RestController
public class UsageController {

    private final UsageService usageService;

    public UsageController(UsageService usageService) {
        this.usageService = usageService;
    }

    @PostMapping("/api/v1/usage")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Long> record(@Valid @RequestBody RecordUsageRequest request) {
        long remaining = usageService.record(
                request.userId(), request.serviceName(), request.tokensUsed(), request.occurredAt());
        return Map.of("remainingCredit", remaining);
    }
}
