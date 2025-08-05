package com.miniSIEM.service;

import com.miniSIEM.model.Alert;
import com.miniSIEM.model.LogEntry;
import com.miniSIEM.repository.AlertRepository;
import com.miniSIEM.repository.LogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BruteForceDetector {
    private final LogRepository logRepository;
    private final AlertRepository alertRepository;
    private final IpWhitelistService ipWhitelist;

    private final int BRUTE_FORCE_THRESHOLD = 5;
    private final int TIME_WINDOW_MINUTES = 5;

    @Scheduled(fixedRate = 60_000)
    public void scanForBruteForce() {
        Instant cutoffTime = Instant.now().minus(TIME_WINDOW_MINUTES, ChronoUnit.MINUTES);

        List<LogEntry> failedLogins = logRepository.findRecentByLevel("ERROR", cutoffTime);


        Map<String, Long> ipAttempts = failedLogins.stream()
                .filter(log -> log.getMessage().toLowerCase().contains("login"))
                .collect(Collectors.groupingBy(
                        LogEntry::getIp,
                        Collectors.counting()
                ));

        ipAttempts.entrySet().stream()
                .filter(entry -> entry.getValue() >= BRUTE_FORCE_THRESHOLD)
                .filter(entry -> ipWhitelist.isAllowed(entry.getKey()))
                .forEach(entry -> {
                    String ip = entry.getKey();
                    if (alertRepository.findBySourceIpAndResolvedFalse(ip).isEmpty()) {
                        Alert alert = Alert.createBruteForceAlert(ip, entry.getValue().intValue());
                        alertRepository.save(alert);
                        log.warn("Brute force alert generated for IP: {}", ip);
                    }
                });
    }
}