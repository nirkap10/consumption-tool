package com.nirkap.consumption.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.nirkap.consumption.entity.UsageEvent;

// The report queries return rows of [key, total tokens], where the key is a
// service name or a user id. The time range is [from, to). Reservations still
// waiting for their usage call (reservedUntil set) are left out.
public interface UsageEventRepository extends JpaRepository<UsageEvent, Long> {

    boolean existsByEventId(String eventId);

    Optional<UsageEvent> findByEventId(String eventId);

    List<UsageEvent> findByReservedUntilBefore(Instant now);

    // Turns a reservation into normal usage. The WHERE makes sure only one call
    // can do it: returns 1 for that call, 0 for a retry or a row already gone.
    @Modifying
    @Query("UPDATE UsageEvent e SET e.tokensUsed = :tokens, e.occurredAt = :occurredAt, e.reservedUntil = NULL "
            + "WHERE e.id = :id AND e.reservedUntil IS NOT NULL")
    int confirmReservation(@Param("id") Long id, @Param("tokens") long tokens,
            @Param("occurredAt") Instant occurredAt);

    // Deletes an expired reservation. Returns 0 if it was confirmed meanwhile.
    @Modifying
    @Query("DELETE FROM UsageEvent e WHERE e.id = :id AND e.reservedUntil IS NOT NULL")
    int deleteReservation(@Param("id") Long id);

    @Query("""
            SELECT e.serviceName, SUM(e.tokensUsed) FROM UsageEvent e
            WHERE e.userId = :userId AND e.occurredAt >= :from AND e.occurredAt < :to
            AND e.reservedUntil IS NULL
            GROUP BY e.serviceName ORDER BY e.serviceName""")
    List<Object[]> sumByServiceForUser(UUID userId, Instant from, Instant to);

    @Query("""
            SELECT e.serviceName, SUM(e.tokensUsed) FROM UsageEvent e
            WHERE e.organizationId = :organizationId AND e.occurredAt >= :from AND e.occurredAt < :to
            AND e.reservedUntil IS NULL
            GROUP BY e.serviceName ORDER BY e.serviceName""")
    List<Object[]> sumByServiceForOrganization(String organizationId, Instant from, Instant to);

    @Query("""
            SELECT e.userId, SUM(e.tokensUsed) FROM UsageEvent e
            WHERE e.organizationId = :organizationId AND e.occurredAt >= :from AND e.occurredAt < :to
            AND e.reservedUntil IS NULL
            GROUP BY e.userId ORDER BY e.userId""")
    List<Object[]> sumByUserForOrganization(String organizationId, Instant from, Instant to);
}
