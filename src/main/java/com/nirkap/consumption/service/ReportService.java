package com.nirkap.consumption.service;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.nirkap.consumption.dto.MonthlyReport;
import com.nirkap.consumption.repository.OrganizationRepository;
import com.nirkap.consumption.repository.UsageEventRepository;

// Months are calendar months in UTC.
@Service
public class ReportService {

    private final UserService userService;
    private final OrganizationRepository organizationRepository;
    private final UsageEventRepository usageEventRepository;

    public ReportService(UserService userService, OrganizationRepository organizationRepository,
            UsageEventRepository usageEventRepository) {
        this.userService = userService;
        this.organizationRepository = organizationRepository;
        this.usageEventRepository = usageEventRepository;
    }

    public MonthlyReport forUser(UUID userId, YearMonth month) {
        userService.get(userId);
        Map<String, Long> byService = toMap(
                usageEventRepository.sumByServiceForUser(userId, start(month), start(month.plusMonths(1))));
        return new MonthlyReport(month.toString(), total(byService), byService, null);
    }

    public MonthlyReport forOrganization(UUID organizationId, YearMonth month) {
        if (!organizationRepository.existsById(organizationId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Organization not found");
        }
        Instant from = start(month);
        Instant to = start(month.plusMonths(1));
        Map<String, Long> byService = toMap(usageEventRepository.sumByServiceForOrganization(organizationId, from, to));
        Map<UUID, Long> byUser = toMap(usageEventRepository.sumByUserForOrganization(organizationId, from, to));
        return new MonthlyReport(month.toString(), total(byService), byService, byUser);
    }

    private static Instant start(YearMonth month) {
        return month.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    // Each row is [key, total tokens].
    @SuppressWarnings("unchecked")
    private static <K> Map<K, Long> toMap(List<Object[]> rows) {
        Map<K, Long> map = new LinkedHashMap<>();
        for (Object[] row : rows) {
            map.put((K) row[0], (Long) row[1]);
        }
        return map;
    }

    private static long total(Map<String, Long> byService) {
        long total = 0;
        for (long tokens : byService.values()) {
            total += tokens;
        }
        return total;
    }
}
