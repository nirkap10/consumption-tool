package com.nirkap.consumption.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.nirkap.consumption.dto.CreateGrantRequest;
import com.nirkap.consumption.service.BalanceResult;
import com.nirkap.consumption.service.GrantService;

import jakarta.validation.Valid;

@RestController
public class GrantController {

    private final GrantService grantService;

    public GrantController(GrantService grantService) {
        this.grantService = grantService;
    }

    // 201 when the grant is new, 200 when this eventId was already recorded.
    @PostMapping("/api/v1/grants")
    public ResponseEntity<Map<String, Long>> create(@Valid @RequestBody CreateGrantRequest request) {
        BalanceResult result = grantService.create(request.eventId(),
                request.userId(), request.amount(), request.reason());
        HttpStatus status = result.duplicate() ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(Map.of("remainingCredit", result.remainingCredit()));
    }
}
