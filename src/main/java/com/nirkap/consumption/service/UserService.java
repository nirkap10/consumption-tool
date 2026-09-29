package com.nirkap.consumption.service;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.nirkap.consumption.entity.User;
import com.nirkap.consumption.repository.OrganizationRepository;
import com.nirkap.consumption.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;

    public UserService(UserRepository userRepository, OrganizationRepository organizationRepository) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
    }

    public User create(UUID organizationId, long monthlyAllowance) {
        if (!organizationRepository.existsById(organizationId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Organization not found");
        }
        // A new user starts with their full allowance as credit.
        return userRepository.save(new User(organizationId, monthlyAllowance, monthlyAllowance));
    }
}
