package com.miniSIEM.service;

import com.miniSIEM.dto.DashboardStats;
import com.miniSIEM.dto.LogActivity;
import com.miniSIEM.dto.ThreatInfo;
import com.miniSIEM.model.LogEntry;
import com.miniSIEM.repository.LogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final LogRepository logRepository;
    private final Map<SseEmitter, String> realTimeClients = new ConcurrentHashMap<>();

    public DashboardStats getDashboardStats() {
        Instant now = Instant.now();
        Instant last24h = now.minus(24, ChronoUnit.HOURS);
        Instant lastHour = now.minus(1, ChronoUnit.HOURS);

        // Get basic counts
        long totalLogs = logRepository.count();
        long logsLast24h = logRepository.countByTimestampAfter(last24h);
        long logsLastHour = logRepository.countByTimestampAfter(lastHour);

        // Calculate logs per minute
        double logsPerMinute = logsLastHour / 60.0;

        // Get logs by level
        Map<String, Long> logsByLevel = new HashMap<>();
        logsByLevel.put("ERROR", logRepository.countByLogLevel("ERROR"));
        logsByLevel.put("WARN", logRepository.countByLogLevel("WARN"));
        logsByLevel.put("INFO", logRepository.countByLogLevel("INFO"));
        logsByLevel.put("DEBUG", logRepository.countByLogLevel("DEBUG"));

        // Get logs by source (simplified)
        Map<String, Long> logsBySource = getTopSourcesAsMap(5);

        // Mock threat detection (we'll implement real detection later)
        long activeThreats = detectActiveThreats();
        long criticalAlerts = logsByLevel.get("ERROR");

        // System status
        String systemStatus = determineSystemStatus(logsPerMinute, activeThreats);

        return DashboardStats.builder()
                .totalLogs(totalLogs)
                .logsLast24h(logsLast24h)
                .logsLastHour(logsLastHour)
                .activeThreats(activeThreats)
                .criticalAlerts(criticalAlerts)
                .logsByLevel(logsByLevel)
                .logsBySource(logsBySource)
                .logsPerMinute(logsPerMinute)
                .systemStatus(systemStatus)
                .lastUpdated(now)
                .build();
    }

    public List<LogActivity> getRecentActivity(int hours) {
        Instant since = Instant.now().minus(hours, ChronoUnit.HOURS);

        // Group logs by hour and level
        List<LogEntry> recentLogs = logRepository.findByTimestampAfter(since);

        Map<String, Long> activityMap = recentLogs.stream()
                .collect(Collectors.groupingBy(
                        log -> log.getTimestamp().truncatedTo(ChronoUnit.HOURS).toString() + "_" + log.getLogLevel(),
                        Collectors.counting()
                ));

        return activityMap.entrySet().stream()
                .map(entry -> {
                    String[] parts = entry.getKey().split("_");
                    return LogActivity.builder()
                            .timestamp(Instant.parse(parts[0]))
                            .level(parts[1])
                            .count(entry.getValue())
                            .build();
                })
                .sorted(Comparator.comparing(LogActivity::getTimestamp).reversed())
                .collect(Collectors.toList());
    }

    public Map<String, Object> getThreatSummary() {
        Map<String, Object> summary = new HashMap<>();

        // Mock threat detection - we'll implement real detection later
        List<ThreatInfo> threats = Arrays.asList(
                ThreatInfo.builder()
                        .id("T001")
                        .type("Brute Force Attack")
                        .severity("HIGH")
                        .description("Multiple failed login attempts detected")
                        .sourceIp("192.168.1.100")
                        .detectedAt(Instant.now().minus(2, ChronoUnit.HOURS))
                        .status("ACTIVE")
                        .riskScore(85)
                        .build(),
                ThreatInfo.builder()
                        .id("T002")
                        .type("Unusual Traffic Pattern")
                        .severity("MEDIUM")
                        .description("Abnormal request volume from single IP")
                        .sourceIp("10.0.1.50")
                        .detectedAt(Instant.now().minus(30, ChronoUnit.MINUTES))
                        .status("INVESTIGATING")
                        .riskScore(65)
                        .build()
        );

        summary.put("threats", threats);
        summary.put("totalThreats", threats.size());
        summary.put("highSeverity", threats.stream().filter(t -> "HIGH".equals(t.getSeverity())).count());
        summary.put("mediumSeverity", threats.stream().filter(t -> "MEDIUM".equals(t.getSeverity())).count());

        return summary;
    }

    public List<Map<String, Object>> getTopSources(int limit) {
        // Get aggregated source data
        List<LogEntry> allLogs = logRepository.findAll();

        Map<String, Long> sourceCounts = allLogs.stream()
                .collect(Collectors.groupingBy(LogEntry::getSource, Collectors.counting()));

        return sourceCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(limit)
                .map(entry -> {
                    Map<String, Object> sourceInfo = new HashMap<>();
                    sourceInfo.put("source", entry.getKey());
                    sourceInfo.put("count", entry.getValue());
                    sourceInfo.put("percentage", (entry.getValue() * 100.0) / allLogs.size());
                    return sourceInfo;
                })
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> getLogTrends(int hours) {
        Instant since = Instant.now().minus(hours, ChronoUnit.HOURS);
        List<LogEntry> logs = logRepository.findByTimestampAfter(since);

        // Group by hour
        Map<String, Long> hourlyTrends = logs.stream()
                .collect(Collectors.groupingBy(
                        log -> log.getTimestamp().truncatedTo(ChronoUnit.HOURS).toString(),
                        Collectors.counting()
                ));

        return hourlyTrends.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> trend = new HashMap<>();
                    trend.put("timestamp", entry.getKey());
                    trend.put("count", entry.getValue());
                    return trend;
                })
                .sorted(Comparator.comparing(t -> (String) t.get("timestamp")))
                .collect(Collectors.toList());
    }

    public void addRealTimeClient(SseEmitter emitter, String username) {
        realTimeClients.put(emitter, username);

        emitter.onCompletion(() -> realTimeClients.remove(emitter));
        emitter.onTimeout(() -> realTimeClients.remove(emitter));
        emitter.onError(e -> {
            log.error("SSE error for user {}: {}", username, e.getMessage());
            realTimeClients.remove(emitter);
        });

        // Send initial connection message
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("Real-time feed connected"));
        } catch (IOException e) {
            log.error("Failed to send initial SSE message", e);
            realTimeClients.remove(emitter);
        }
    }

    @Async
    public void broadcastNewLog(LogEntry logEntry) {
        if (realTimeClients.isEmpty()) return;

        Map<String, Object> logData = new HashMap<>();
        logData.put("timestamp", logEntry.getTimestamp());
        logData.put("level", logEntry.getLogLevel());
        logData.put("source", logEntry.getSource());
        logData.put("message", logEntry.getMessage());
        logData.put("ip", logEntry.getIp());

        List<SseEmitter> deadEmitters = new ArrayList<>();

        realTimeClients.forEach((emitter, username) -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("newLog")
                        .data(logData));
            } catch (IOException e) {
                log.debug("SSE client disconnected: {}", username);
                deadEmitters.add(emitter);
            }
        });

        // Remove dead connections
        deadEmitters.forEach(realTimeClients::remove);
    }

    public byte[] exportData(String format, String dateRange) {
        // Mock export functionality - implement actual export logic
        String exportContent = "timestamp,level,source,message,ip\n";

        List<LogEntry> logs = getLogsForDateRange(dateRange);
        for (LogEntry log : logs) {
            exportContent += String.format("%s,%s,%s,\"%s\",%s\n",
                    log.getTimestamp(),
                    log.getLogLevel(),
                    log.getSource(),
                    log.getMessage().replace("\"", "\"\""),
                    log.getIp());
        }

        return exportContent.getBytes();
    }

    public Map<String, Object> getSystemHealth() {
        Map<String, Object> health = new HashMap<>();

        // Basic health metrics
        health.put("status", "UP");
        health.put("uptime", System.currentTimeMillis());
        health.put("memory", getMemoryInfo());
        health.put("database", "CONNECTED");
        health.put("activeConnections", realTimeClients.size());

        return health;
    }

    // Helper methods
    private Map<String, Long> getTopSourcesAsMap(int limit) {
        return getTopSources(limit).stream()
                .collect(Collectors.toMap(
                        map -> (String) map.get("source"),
                        map -> (Long) map.get("count"),
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
    }

    private long detectActiveThreats() {
        // Simple threat detection - count error logs in last hour
        Instant lastHour = Instant.now().minus(1, ChronoUnit.HOURS);
        List<LogEntry> errorLogs = logRepository.findRecentByLevel("ERROR", lastHour);

        // If more than 10 errors in an hour, consider it a threat
        return errorLogs.size() > 10 ? 1 : 0;
    }

    private String determineSystemStatus(double logsPerMinute, long activeThreats) {
        if (activeThreats > 0) return "ALERT";
        if (logsPerMinute > 100) return "BUSY";
        if (logsPerMinute < 1) return "QUIET";
        return "NORMAL";
    }

    private List<LogEntry> getLogsForDateRange(String dateRange) {
        Instant since = switch (dateRange) {
            case "1h" -> Instant.now().minus(1, ChronoUnit.HOURS);
            case "24h" -> Instant.now().minus(24, ChronoUnit.HOURS);
            case "7d" -> Instant.now().minus(7, ChronoUnit.DAYS);
            default -> Instant.now().minus(24, ChronoUnit.HOURS);
        };

        return logRepository.findByTimestampAfter(since);
    }

    private Map<String, Object> getMemoryInfo() {
        Runtime runtime = Runtime.getRuntime();
        Map<String, Object> memory = new HashMap<>();
        memory.put("total", runtime.totalMemory());
        memory.put("free", runtime.freeMemory());
        memory.put("used", runtime.totalMemory() - runtime.freeMemory());
        memory.put("max", runtime.maxMemory());
        return memory;
    }
}