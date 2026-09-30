package com.nirkap.consumption.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nirkap.consumption.service.ResetService;

@RestController
public class AdminController {

    private final ResetService resetService;

    public AdminController(ResetService resetService) {
        this.resetService = resetService;
    }

    // Runs the monthly reset now, so it can be tested without waiting a month.
    @PostMapping("/api/v1/admin/reset")
    public Map<String, Integer> reset() {
        return Map.of("usersReset", resetService.resetAll());
    }
}
