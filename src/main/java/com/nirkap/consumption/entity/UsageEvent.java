package com.nirkap.consumption.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "usage_events")
public class UsageEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private UUID organizationId;

    @Column(nullable = false)
    private String serviceName;

    @Column(nullable = false)
    private long tokensUsed;

    @Column(nullable = false)
    private Instant occurredAt;

    protected UsageEvent() {
    }

    public UsageEvent(UUID userId, UUID organizationId, String serviceName, long tokensUsed, Instant occurredAt) {
        this.userId = userId;
        this.organizationId = organizationId;
        this.serviceName = serviceName;
        this.tokensUsed = tokensUsed;
        this.occurredAt = occurredAt;
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

    public String getServiceName() {
        return serviceName;
    }

    public long getTokensUsed() {
        return tokensUsed;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
