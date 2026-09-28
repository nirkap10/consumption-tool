package com.nirkap.consumption.service;

import org.springframework.stereotype.Service;

import com.nirkap.consumption.entity.Organization;
import com.nirkap.consumption.repository.OrganizationRepository;

@Service
public class OrganizationService {

    private final OrganizationRepository organizationRepository;

    public OrganizationService(OrganizationRepository organizationRepository) {
        this.organizationRepository = organizationRepository;
    }

    public Organization create(String name) {
        return organizationRepository.save(new Organization(name));
    }
}
