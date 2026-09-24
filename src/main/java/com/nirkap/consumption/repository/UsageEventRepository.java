package com.nirkap.consumption.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nirkap.consumption.entity.UsageEvent;

public interface UsageEventRepository extends JpaRepository<UsageEvent, Long> {
}
