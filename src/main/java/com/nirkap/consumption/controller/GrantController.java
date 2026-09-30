package com.nirkap.consumption.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.nirkap.consumption.dto.CreateGrantRequest;
import com.nirkap.consumption.service.GrantService;

import jakarta.validation.Valid;

@RestController
public class GrantController {

    private final GrantService grantService;

    public GrantController(GrantService grantService) {
        this.grantService = grantService;
    }

    @PostMapping("/api/v1/grants")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Long> create(@Valid @RequestBody CreateGrantRequest request) {
        long remaining = grantService.create(request.userId(), request.amount(), request.reason());
        return Map.of("remainingCredit", remaining);
    }
}
