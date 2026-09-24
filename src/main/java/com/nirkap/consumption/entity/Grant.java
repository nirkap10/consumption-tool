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
@Table(name = "grants")
public class Grant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private UUID organizationId;

    @Column(nullable = false)
    private long amount;

    private String reason;

    @CreationTimestamp
    private Instant grantedAt;

    protected Grant() {
    }

    public Grant(UUID userId, UUID organizationId, long amount, String reason) {
        this.userId = userId;
        this.organizationId = organizationId;
        this.amount = amount;
        this.reason = reason;
    }

    public Long getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public long getAmount() {
        return amount;
    }

    public String getReason() {
        return reason;
    }

    public Instant getGrantedAt() {
        return grantedAt;
    }
}
