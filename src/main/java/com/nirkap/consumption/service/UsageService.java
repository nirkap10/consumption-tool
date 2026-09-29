package com.nirkap.consumption.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.nirkap.consumption.entity.UsageEvent;
import com.nirkap.consumption.entity.User;
import com.nirkap.consumption.repository.UsageEventRepository;
import com.nirkap.consumption.repository.UserRepository;

@Service
public class UsageService {

    private final UserService userService;
    private final UserRepository userRepository;
    private final UsageEventRepository usageEventRepository;

    public UsageService(UserService userService, UserRepository userRepository,
            UsageEventRepository usageEventRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.usageEventRepository = usageEventRepository;
    }

    // Always records, even without enough credit. Two separate writes, no
    // transaction yet (see DECISIONS).
    public long record(UUID userId, String serviceName, long tokensUsed, Instant occurredAt) {
        User user = userService.get(userId);
        Instant when = occurredAt != null ? occurredAt : Instant.now();

        usageEventRepository.save(
                new UsageEvent(user.getId(), user.getOrganizationId(), serviceName, tokensUsed, when));

        user.spend(tokensUsed);
        userRepository.save(user);
        return user.getRemainingCredit();
    }
}
