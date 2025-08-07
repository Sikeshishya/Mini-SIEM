package com.miniSIEM.controller;

import com.miniSIEM.service.DashboardService;
import com.miniSIEM.dto.DashboardStats;
import com.miniSIEM.dto.LogActivity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ANALYST') or hasRole('VIEWER')")
    public ResponseEntity<DashboardStats> getDashboardStats(Authentication authentication) {
        log.debug("Dashboard stats request from user: {}", authentication.getName());
        DashboardStats stats = dashboardService.getDashboardStats();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/activity")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ANALYST') or hasRole('VIEWER')")
    public ResponseEntity<List<LogActivity>> getRecentActivity(
            @RequestParam(defaultValue = "24") int hours,
            Authentication authentication) {

        log.debug("Recent activity request from user: {} for {} hours",
                authentication.getName(), hours);

        List<LogActivity> activity = dashboardService.getRecentActivity(hours);
        return ResponseEntity.ok(activity);
    }

    @GetMapping("/threats")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ANALYST') or hasRole('VIEWER')")
    public ResponseEntity<?> getThreatSummary(Authentication authentication) {
        log.debug("Threat summary request from user: {}", authentication.getName());

        Map<String, Object> threats = dashboardService.getThreatSummary();
        return ResponseEntity.ok(threats);
    }

    @GetMapping("/top-sources")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ANALYST') or hasRole('VIEWER')")
    public ResponseEntity<?> getTopSources(@RequestParam(defaultValue = "10") int limit,
                                           Authentication authentication) {
        log.debug("Top sources request from user: {}", authentication.getName());

        List<Map<String, Object>> topSources = dashboardService.getTopSources(limit);
        return ResponseEntity.ok(topSources);
    }

    @GetMapping("/log-trends")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ANALYST') or hasRole('VIEWER')")
    public ResponseEntity<?> getLogTrends(@RequestParam(defaultValue = "24") int hours,
                                          Authentication authentication) {
        log.debug("Log trends request from user: {}", authentication.getName());

        List<Map<String, Object>> trends = dashboardService.getLogTrends(hours);
        return ResponseEntity.ok(trends);
    }

    @GetMapping(value = "/realtime", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasRole('ADMIN') or hasRole('ANALYST') or hasRole('VIEWER')")
    public SseEmitter getRealTimeLogs(Authentication authentication) {
        log.info("Real-time stream connection from user: {}", authentication.getName());

        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        dashboardService.addRealTimeClient(emitter, authentication.getName());

        return emitter;
    }

    @PostMapping("/export")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ANALYST')")
    public ResponseEntity<?> exportData(@RequestBody Map<String, Object> exportRequest,
                                        Authentication authentication) {
        log.info("Data export request from user: {}", authentication.getName());

        String format = (String) exportRequest.getOrDefault("format", "csv");
        String dateRange = (String) exportRequest.getOrDefault("dateRange", "24h");

        byte[] exportData = dashboardService.exportData(format, dateRange);

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=siem-export." + format)
                .header("Content-Type", "application/octet-stream")
                .body(exportData);
    }

    @GetMapping("/health")
    public ResponseEntity<?> getSystemHealth() {
        Map<String, Object> health = dashboardService.getSystemHealth();
        return ResponseEntity.ok(health);
    }
}