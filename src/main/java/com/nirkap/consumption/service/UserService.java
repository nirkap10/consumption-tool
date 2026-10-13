package com.nirkap.consumption.service;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.nirkap.consumption.entity.User;
import com.nirkap.consumption.repository.OrganizationRepository;
import com.nirkap.consumption.repository.UserRepository;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;

    public UserService(UserRepository userRepository, OrganizationRepository organizationRepository) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
    }

    public User create(String organizationId, long monthlyAllowance) {
        if (!organizationRepository.existsById(organizationId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Organization not found: " + organizationId);
        }
        // A new user starts with their full allowance as credit.
        User user = userRepository.save(new User(organizationId, monthlyAllowance, monthlyAllowance));
        log.info("User created: id={} organizationId={} monthlyAllowance={}",
                user.getId(), organizationId, monthlyAllowance);
        return user;
    }

    public User get(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + userId));
    }
}
