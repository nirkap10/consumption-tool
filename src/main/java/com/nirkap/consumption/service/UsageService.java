package com.nirkap.consumption.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nirkap.consumption.entity.UsageEvent;
import com.nirkap.consumption.entity.User;
import com.nirkap.consumption.repository.UsageEventRepository;
import com.nirkap.consumption.repository.UserRepository;

@Service
public class UsageService {

    // duplicate is true when this eventId was already recorded and nothing changed.
    public record Result(long remainingCredit, boolean duplicate) {
    }

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
    // A repeated eventId is a retry of a report we already have, so it changes
    // nothing and just returns the current balance.
    @Transactional
    public Result record(String eventId, UUID userId, String serviceName, long tokensUsed, Instant occurredAt) {
        User user = userService.get(userId);

        if (usageEventRepository.existsByEventId(eventId)) {
            return new Result(userRepository.findRemainingCredit(user.getId()), true);
        }

        Instant when = occurredAt != null ? occurredAt : Instant.now();
        usageEventRepository.save(
                new UsageEvent(eventId, user.getId(), user.getOrganizationId(), serviceName, tokensUsed, when));

        userRepository.spend(user.getId(), tokensUsed);
        return new Result(userRepository.findRemainingCredit(user.getId()), false);
    }
}
