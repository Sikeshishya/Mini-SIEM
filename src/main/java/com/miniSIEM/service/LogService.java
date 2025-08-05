package com.miniSIEM.service;

import com.miniSIEM.model.LogEntry;
import com.miniSIEM.repository.LogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class LogService {
    private final LogRepository logRepository;

    public LogEntry saveLog(LogEntry logEntry) {
        // Set timestamp to current time if not provided
        if (logEntry.getTimestamp() == null) {
            logEntry.setTimestamp(Instant.now());
        }

        // Input validation and sanitization
        validateLogEntry(logEntry);

        LogEntry savedLog = logRepository.save(logEntry);
        log.debug("Log entry saved: {}", savedLog.getId());

        return savedLog;
    }

    public List<LogEntry> saveBulkLogs(List<LogEntry> logEntries) {
        log.info("Processing bulk log insertion: {} entries", logEntries.size());

        // Validate all entries
        logEntries.forEach(this::validateLogEntry);

        // Set timestamps for entries that don't have them
        Instant now = Instant.now();
        logEntries.forEach(entry -> {
            if (entry.getTimestamp() == null) {
                entry.setTimestamp(now);
            }
        });

        List<LogEntry> savedLogs = logRepository.saveAll(logEntries);
        log.info("Bulk log insertion completed: {} entries saved", savedLogs.size());

        return savedLogs;
    }

    public List<LogEntry> findLogs(String ip, String logLevel, String source, int page, int size) {
        // Validate pagination parameters
        if (page < 0) page = 0;
        if (size < 1 || size > 1000) size = 100; // Limit page size to prevent abuse

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));

        // If no filters provided, return paginated results
        if (ip == null && logLevel == null && source == null) {
            return logRepository.findAll(pageable).getContent();
        }

        // Apply filters based on provided parameters
        if (ip != null && logLevel == null && source == null) {
            return logRepository.findByIp(ip, pageable);
        }

        if (logLevel != null && ip == null && source == null) {
            return logRepository.findByLogLevel(logLevel, pageable);
        }

        if (source != null && ip == null && logLevel == null) {
            return logRepository.findBySource(source, pageable);
        }

        // Multiple filters - use custom query method
        return logRepository.findByFilters(ip, logLevel, source, pageable);
    }

    public LogEntry findById(String id) {
        return logRepository.findById(id).orElse(null);
    }

    public boolean deleteLog(String id) {
        if (logRepository.existsById(id)) {
            logRepository.deleteById(id);
            log.warn("Log entry deleted: {}", id);
            return true;
        }
        return false;
    }

    public Map<String, Object> getLogStatistics() {
        Map<String, Object> stats = new HashMap<>();

        // Total log count
        long totalLogs = logRepository.count();
        stats.put("totalLogs", totalLogs);

        // Logs in last 24 hours
        Instant last24h = Instant.now().minus(24, ChronoUnit.HOURS);
        long logsLast24h = logRepository.countByTimestampAfter(last24h);
        stats.put("logsLast24h", logsLast24h);

        // Logs by level
        Map<String, Long> logsByLevel = new HashMap<>();
        logsByLevel.put("ERROR", logRepository.countByLogLevel("ERROR"));
        logsByLevel.put("WARN", logRepository.countByLogLevel("WARN"));
        logsByLevel.put("INFO", logRepository.countByLogLevel("INFO"));
        logsByLevel.put("DEBUG", logRepository.countByLogLevel("DEBUG"));
        stats.put("logsByLevel", logsByLevel);

        // Top sources
        List<Map<String, Object>> topSources = logRepository.findTopSources();
        stats.put("topSources", topSources);

        // Recent activity (last hour)
        Instant lastHour = Instant.now().minus(1, ChronoUnit.HOURS);
        long recentActivity = logRepository.countByTimestampAfter(lastHour);
        stats.put("recentActivity", recentActivity);

        return stats;
    }

    private void validateLogEntry(LogEntry logEntry) {
        if (logEntry.getMessage() == null || logEntry.getMessage().trim().isEmpty()) {
            throw new IllegalArgumentException("Log message cannot be empty");
        }

        if (logEntry.getSource() == null || logEntry.getSource().trim().isEmpty()) {
            throw new IllegalArgumentException("Log source cannot be empty");
        }

        if (logEntry.getLogLevel() == null || logEntry.getLogLevel().trim().isEmpty()) {
            throw new IllegalArgumentException("Log level cannot be empty");
        }

        // Sanitize inputs to prevent injection attacks
        logEntry.setMessage(sanitizeInput(logEntry.getMessage()));
        logEntry.setSource(sanitizeInput(logEntry.getSource()));
        logEntry.setLogLevel(sanitizeInput(logEntry.getLogLevel()));

        if (logEntry.getIp() != null) {
            logEntry.setIp(sanitizeInput(logEntry.getIp()));
            validateIpAddress(logEntry.getIp());
        }
    }

    private String sanitizeInput(String input) {
        if (input == null) return null;

        // Remove potential script tags and SQL injection attempts
        return input
                .replaceAll("<script[^>]*>.*?</script>", "")
                .replaceAll("(?i)(javascript:|vbscript:|onload=)", "")
                .replaceAll("(?i)(union|select|insert|delete|update|drop|create|alter)", "")
                .trim();
    }

    private void validateIpAddress(String ip) {
        if (ip == null || ip.trim().isEmpty()) return;

        // Basic IP validation (IPv4)
        String ipPattern = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";
        if (!ip.matches(ipPattern)) {
            // Could be IPv6 or hostname, allow it but log for review
            log.debug("Non-standard IP format detected: {}", ip);
        }
    }
}