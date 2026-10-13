package com.nirkap.consumption.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.nirkap.consumption.entity.User;
import com.nirkap.consumption.repository.UserRepository;

@Service
public class ResetService {

    private static final Logger log = LoggerFactory.getLogger(ResetService.class);

    private final UserRepository userRepository;

    public ResetService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Midnight UTC on the 1st of every month, matching the report's UTC months.
    @Scheduled(cron = "0 0 0 1 * *", zone = "UTC")
    public void monthlyReset() {
        log.info("Scheduled monthly reset starting");
        resetAll();
    }

    // Returns how many users were reset.
    public int resetAll() {
        List<User> users = userRepository.findAll();
        for (User user : users) {
            user.resetCredit();
        }
        userRepository.saveAll(users);
        log.info("Monthly reset done: usersReset={}", users.size());
        return users.size();
    }
}
