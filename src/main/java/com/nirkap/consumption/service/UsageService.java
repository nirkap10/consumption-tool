package com.nirkap.consumption.service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nirkap.consumption.entity.UsageEvent;
import com.nirkap.consumption.entity.User;
import com.nirkap.consumption.repository.UsageEventRepository;
import com.nirkap.consumption.repository.UserRepository;

@Service
public class UsageService {

    private static final Logger log = LoggerFactory.getLogger(UsageService.class);

    private final UserService userService;
    private final UserRepository userRepository;
    private final UsageEventRepository usageEventRepository;

    public UsageService(UserService userService, UserRepository userRepository,
            UsageEventRepository usageEventRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.usageEventRepository = usageEventRepository;
    }

    // Always records, even without enough credit (see DECISIONS). The event row
    // and the balance change are one transaction: both are saved or neither is.
    // If the eventId belongs to a waiting reservation, this report confirms it.
    // Otherwise a repeated eventId is a retry of a report we already have, so it
    // changes nothing and just returns the current balance.
    @Transactional
    public BalanceResult record(String eventId, UUID userId, String serviceName, long tokensUsed, Instant occurredAt) {
        User user = userService.get(userId);
        Instant when = occurredAt != null ? occurredAt : Instant.now();

        Optional<UsageEvent> existing = usageEventRepository.findByEventId(eventId);
        if (existing.isPresent()) {
            UsageEvent event = existing.get();
            if (usageEventRepository.confirmReservation(event.getId(), tokensUsed, when) == 1) {
                // The reserved tokens were taken already; charge only the difference.
                userRepository.spend(event.getUserId(), tokensUsed - event.getTokensUsed());
                long remaining = userRepository.findRemainingCredit(event.getUserId());
                log.info("Reservation confirmed: eventId={} userId={} reserved={} tokensUsed={} remainingCredit={}",
                        eventId, event.getUserId(), event.getTokensUsed(), tokensUsed, remaining);
                return new BalanceResult(remaining, false);
            }
            long remaining = userRepository.findRemainingCredit(user.getId());
            log.info("Usage repeated, nothing changed: eventId={} userId={} remainingCredit={}",
                    eventId, userId, remaining);
            return new BalanceResult(remaining, true);
        }

        usageEventRepository.save(
                new UsageEvent(eventId, user.getId(), user.getOrganizationId(), serviceName, tokensUsed, when));

        userRepository.spend(user.getId(), tokensUsed);
        long remaining = userRepository.findRemainingCredit(user.getId());
        log.info("Usage recorded: eventId={} userId={} service={} tokensUsed={} remainingCredit={}",
                eventId, userId, serviceName, tokensUsed, remaining);
        return new BalanceResult(remaining, false);
    }
}
