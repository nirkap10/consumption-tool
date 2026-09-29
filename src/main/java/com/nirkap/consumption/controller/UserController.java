package com.nirkap.consumption.controller;

import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.nirkap.consumption.dto.CreateUserRequest;
import com.nirkap.consumption.service.UserService;

import jakarta.validation.Valid;

@RestController
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/api/v1/users")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, UUID> create(@Valid @RequestBody CreateUserRequest request) {
        return Map.of("id", userService.create(request.organizationId(), request.monthlyAllowance()).getId());
    }

    @GetMapping("/api/v1/users/{userId}/balance")
    public Map<String, Long> balance(@PathVariable UUID userId) {
        return Map.of("remainingCredit", userService.get(userId).getRemainingCredit());
    }
}
