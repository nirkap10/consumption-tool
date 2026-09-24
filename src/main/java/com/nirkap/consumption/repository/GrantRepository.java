package com.nirkap.consumption.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nirkap.consumption.entity.Grant;

public interface GrantRepository extends JpaRepository<Grant, Long> {
}
