package com.nirkap.consumption.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.nirkap.consumption.entity.UsageEvent;
import com.nirkap.consumption.entity.User;
import com.nirkap.consumption.repository.UsageEventRepository;
import com.nirkap.consumption.repository.UserRepository;

// A reservation is a usage_events row written before the use, with
// reservedUntil set. POST /usage with the same eventId confirms it (see
// UsageService); if that does not come in time, expireReservations() gives the
// tokens back.
@Service
public class ReservationService {

    private static final Logger log = LoggerFactory.getLogger(ReservationService.class);

    private final UserService userService;
    private final UserRepository userRepository;
    private final UsageEventRepository usageEventRepository;
    private final Duration reservationTime;

    public ReservationService(UserService userService, UserRepository userRepository,
            UsageEventRepository usageEventRepository,
            @Value("${consumption.reservation-minutes}") long reservationMinutes) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.usageEventRepository = usageEventRepository;
        this.reservationTime = Duration.ofMinutes(reservationMinutes);
    }

    // Takes the tokens only if there are enough, otherwise 409 and nothing is
    // written. A repeated eventId changes nothing and returns the current balance.
    @Transactional
    public BalanceResult reserve(String eventId, UUID userId, String serviceName, long tokens) {
        User user = userService.get(userId);

        if (usageEventRepository.existsByEventId(eventId)) {
            long remaining = userRepository.findRemainingCredit(user.getId());
            log.info("Reservation repeated, nothing changed: eventId={} userId={} remainingCredit={}",
                    eventId, userId, remaining);
            return new BalanceResult(remaining, true);
        }

        if (userRepository.reserve(user.getId(), tokens) == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Not enough credit to reserve " + tokens + " tokens for user " + userId);
        }

        Instant now = Instant.now();
        usageEventRepository.save(new UsageEvent(eventId, user.getId(), user.getOrganizationId(), serviceName,
                tokens, now, now.plus(reservationTime)));
        long remaining = userRepository.findRemainingCredit(user.getId());
        log.info("Reservation made: eventId={} userId={} service={} tokens={} remainingCredit={}",
                eventId, userId, serviceName, tokens, remaining);
        return new BalanceResult(remaining, false);
    }

    // Every 30 seconds: deletes reservations whose time has passed and gives
    // their tokens back. Returns how many were expired.
    @Scheduled(fixedDelay = 30_000)
    @Transactional
    public int expireReservations() {
        List<UsageEvent> expired = usageEventRepository.findByReservedUntilBefore(Instant.now());
        int count = 0;
        for (UsageEvent event : expired) {
            // 0 means a usage call confirmed it after we loaded it; leave it.
            if (usageEventRepository.deleteReservation(event.getId()) == 1) {
                userRepository.addCredit(event.getUserId(), event.getTokensUsed());
                log.info("Reservation expired, tokens returned: eventId={} userId={} tokens={}",
                        event.getEventId(), event.getUserId(), event.getTokensUsed());
                count++;
            }
        }
        return count;
    }
}
