package com.nirkap.consumption.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.nirkap.consumption.entity.Organization;
import com.nirkap.consumption.repository.OrganizationRepository;

@Service
public class OrganizationService {

    private static final Logger log = LoggerFactory.getLogger(OrganizationService.class);

    private final OrganizationRepository organizationRepository;

    public OrganizationService(OrganizationRepository organizationRepository) {
        this.organizationRepository = organizationRepository;
    }

    public Organization create(String id, String name) {
        // Without this check, save() would quietly overwrite the existing one.
        if (organizationRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Organization id already exists: " + id);
        }
        Organization organization = organizationRepository.save(new Organization(id, name));
        log.info("Organization created: id={} name={}", id, name);
        return organization;
    }
}
