package com.nirkap.consumption.controller;

import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.nirkap.consumption.dto.MonthlyReport;
import com.nirkap.consumption.service.ReportService;

@RestController
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    // Exactly one of userId / organizationId. month is YYYY-MM and defaults to
    // the current month.
    @GetMapping("/api/v1/reports/monthly")
    public MonthlyReport monthly(@RequestParam(required = false) UUID userId,
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(required = false) YearMonth month) {
        if ((userId == null) == (organizationId == null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Give exactly one of userId or organizationId");
        }
        YearMonth reportMonth = month != null ? month : YearMonth.now(ZoneOffset.UTC);
        return userId != null
                ? reportService.forUser(userId, reportMonth)
                : reportService.forOrganization(organizationId, reportMonth);
    }
}
