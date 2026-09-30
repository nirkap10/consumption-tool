package com.nirkap.consumption.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.nirkap.consumption.entity.Grant;
import com.nirkap.consumption.entity.User;
import com.nirkap.consumption.repository.GrantRepository;
import com.nirkap.consumption.repository.UserRepository;

@Service
public class GrantService {

    private final UserService userService;
    private final UserRepository userRepository;
    private final GrantRepository grantRepository;

    public GrantService(UserService userService, UserRepository userRepository,
            GrantRepository grantRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.grantRepository = grantRepository;
    }

    // Two separate writes, no transaction yet (see DECISIONS).
    public long create(UUID userId, long amount, String reason) {
        User user = userService.get(userId);

        grantRepository.save(new Grant(user.getId(), user.getOrganizationId(), amount, reason));

        user.grant(amount);
        userRepository.save(user);
        return user.getRemainingCredit();
    }
}
