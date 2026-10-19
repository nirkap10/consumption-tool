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

    // The caller's id for this report; unique, so a retry is stored only once.
    // Null on rows from before it was added.
    private String eventId;//idempotent

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String organizationId;

    @Column(nullable = false)
    private String serviceName;

    @Column(nullable = false)
    private long tokensUsed;

    @Column(nullable = false)
    private Instant occurredAt;

    // Set while this row is a reservation waiting for its usage call. Null on
    // normal usage.
    private Instant reservedUntil;

    protected UsageEvent() {
    }

    public UsageEvent(String eventId, UUID userId, String organizationId, String serviceName, long tokensUsed,
            Instant occurredAt) {
        this(eventId, userId, organizationId, serviceName, tokensUsed, occurredAt, null);
    }

    public UsageEvent(String eventId, UUID userId, String organizationId, String serviceName, long tokensUsed,
            Instant occurredAt, Instant reservedUntil) {
        this.eventId = eventId;
        this.userId = userId;
        this.organizationId = organizationId;
        this.serviceName = serviceName;
        this.tokensUsed = tokensUsed;
        this.occurredAt = occurredAt;
        this.reservedUntil = reservedUntil;
    }

    public Long getId() {
        return id;
    }

    public String getEventId() {
        return eventId;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getOrganizationId() {
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

    public Instant getReservedUntil() {
        return reservedUntil;
    }
}
