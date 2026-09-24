package com.nirkap.consumption.entity;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID organizationId;

    @Column(nullable = false)
    private long monthlyAllowance;

    @Column(nullable = false)
    private long remainingCredit;

    @CreationTimestamp
    private Instant createdAt;

    protected User() {
    }

    public User(UUID organizationId, long monthlyAllowance, long remainingCredit) {
        this.organizationId = organizationId;
        this.monthlyAllowance = monthlyAllowance;
        this.remainingCredit = remainingCredit;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public long getMonthlyAllowance() {
        return monthlyAllowance;
    }

    public long getRemainingCredit() {
        return remainingCredit;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
