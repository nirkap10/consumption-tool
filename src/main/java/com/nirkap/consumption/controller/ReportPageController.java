package com.nirkap.consumption.controller;

import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.UUID;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import com.nirkap.consumption.dto.MonthlyReport;
import com.nirkap.consumption.service.ReportService;

// The report page for the client. It returns HTML (templates/report.html), not
// JSON. With no id it shows only the form. Errors are shown on the page, so they
// are caught here instead of reaching ErrorHandler.
@Controller
public class ReportPageController {

    private final ReportService reportService;

    public ReportPageController(ReportService reportService) {
        this.reportService = reportService;
    }

    // type is "user" or "organization". month is YYYY-MM and defaults to the
    // current month.
    @GetMapping("/reports")
    public String page(@RequestParam(defaultValue = "user") String type,
            @RequestParam(required = false) String id,
            @RequestParam(required = false) String month,
            Model model) {
        String monthText = (month == null || month.isBlank()) ? YearMonth.now(ZoneOffset.UTC).toString() : month;
        model.addAttribute("type", type);
        model.addAttribute("id", id);
        model.addAttribute("month", monthText);
        if (id == null || id.isBlank()) {
            return "report";
        }
        try {
            YearMonth reportMonth = YearMonth.parse(monthText);
            MonthlyReport report = type.equals("organization")
                    ? reportService.forOrganization(id.trim(), reportMonth)
                    : reportService.forUser(UUID.fromString(id.trim()), reportMonth);
            model.addAttribute("report", report);
        } catch (DateTimeParseException e) {
            model.addAttribute("error", "The month must look like 2026-10.");
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", "That is not a valid user id.");
        } catch (ResponseStatusException e) {
            model.addAttribute("error", e.getReason());
        }
        return "report";
    }
}
