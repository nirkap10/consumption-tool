package com.nirkap.consumption.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.nirkap.consumption.entity.UsageEvent;

// The report queries return rows of [key, total tokens], where the key is a
// service name or a user id. The time range is [from, to).
public interface UsageEventRepository extends JpaRepository<UsageEvent, Long> {

    boolean existsByEventId(String eventId);

    @Query("""
            SELECT e.serviceName, SUM(e.tokensUsed) FROM UsageEvent e
            WHERE e.userId = :userId AND e.occurredAt >= :from AND e.occurredAt < :to
            GROUP BY e.serviceName ORDER BY e.serviceName""")
    List<Object[]> sumByServiceForUser(UUID userId, Instant from, Instant to);

    @Query("""
            SELECT e.serviceName, SUM(e.tokensUsed) FROM UsageEvent e
            WHERE e.organizationId = :organizationId AND e.occurredAt >= :from AND e.occurredAt < :to
            GROUP BY e.serviceName ORDER BY e.serviceName""")
    List<Object[]> sumByServiceForOrganization(UUID organizationId, Instant from, Instant to);

    @Query("""
            SELECT e.userId, SUM(e.tokensUsed) FROM UsageEvent e
            WHERE e.organizationId = :organizationId AND e.occurredAt >= :from AND e.occurredAt < :to
            GROUP BY e.userId ORDER BY e.userId""")
    List<Object[]> sumByUserForOrganization(UUID organizationId, Instant from, Instant to);
}
