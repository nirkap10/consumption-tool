package com.nirkap.consumption.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.nirkap.consumption.dto.RecordUsageRequest;
import com.nirkap.consumption.service.BalanceResult;
import com.nirkap.consumption.service.UsageService;

import jakarta.validation.Valid;

@RestController
public class UsageController {

    private final UsageService usageService;

    public UsageController(UsageService usageService) {
        this.usageService = usageService;
    }

    // 201 when the report is new, 200 when this eventId was already recorded.
    @PostMapping("/api/v1/usage")
    public ResponseEntity<Map<String, Long>> record(@Valid @RequestBody RecordUsageRequest request) {
        BalanceResult result = usageService.record(request.eventId(),
                request.userId(), request.serviceName(), request.tokensUsed(), request.occurredAt());
        HttpStatus status = result.duplicate() ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(Map.of("remainingCredit", result.remainingCredit()));
    }
}
