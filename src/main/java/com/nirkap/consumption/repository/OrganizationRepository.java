package com.nirkap.consumption.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nirkap.consumption.entity.Organization;

public interface OrganizationRepository extends JpaRepository<Organization, UUID> {
}
