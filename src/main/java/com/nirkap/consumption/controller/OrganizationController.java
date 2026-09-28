package com.nirkap.consumption.controller;

import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.nirkap.consumption.dto.CreateOrganizationRequest;
import com.nirkap.consumption.service.OrganizationService;

import jakarta.validation.Valid;

@RestController
public class OrganizationController {

    private final OrganizationService organizationService;

    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @PostMapping("/api/v1/organizations")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, UUID> create(@Valid @RequestBody CreateOrganizationRequest request) {
        return Map.of("id", organizationService.create(request.name()).getId());
    }
}
