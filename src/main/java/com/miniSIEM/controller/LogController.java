package com.miniSIEM.controller;

import com.miniSIEM.model.LogEntry;
import com.miniSIEM.service.LogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
@Slf4j
public class LogController {
    private final LogService logService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('ANALYST')")
    public ResponseEntity<LogEntry> ingestLog(@Valid @RequestBody LogEntry logEntry,
                                              Authentication authentication) {
        log.info("Log ingestion request from user: {}", authentication.getName());
        LogEntry savedLog = logService.saveLog(logEntry);
        log.debug("Log saved with ID: {}", savedLog.getId());
        return ResponseEntity.ok(savedLog);
    }

    @PostMapping("/bulk")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ANALYST')")
    public ResponseEntity<?> ingestBulkLogs(@Valid @RequestBody List<LogEntry> logEntries,
                                            Authentication authentication) {
        log.info("Bulk log ingestion request from user: {} - {} logs",
                authentication.getName(), logEntries.size());

        List<LogEntry> savedLogs = logService.saveBulkLogs(logEntries);

        return ResponseEntity.ok(Map.of(
                "message", "Logs ingested successfully",
                "count", savedLogs.size(),
                "processedBy", authentication.getName()
        ));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('ANALYST') or hasRole('VIEWER')")
    public ResponseEntity<List<LogEntry>> getLogs(
            @RequestParam(required = false) String ip,
            @RequestParam(required = false) String logLevel,
            @RequestParam(required = false) String source,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size,
            Authentication authentication) {

        log.debug("Log query request from user: {} - ip:{}, level:{}, source:{}",
                authentication.getName(), ip, logLevel, source);

        List<LogEntry> logs = logService.findLogs(ip, logLevel, source, page, size);
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ANALYST') or hasRole('VIEWER')")
    public ResponseEntity<LogEntry> getLogById(@PathVariable String id,
                                               Authentication authentication) {
        log.debug("Log retrieval request from user: {} for ID: {}",
                authentication.getName(), id);

        LogEntry log = logService.findById(id);
        if (log != null) {
            return ResponseEntity.ok(log);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ANALYST') or hasRole('VIEWER')")
    public ResponseEntity<?> getLogStatistics(Authentication authentication) {
        log.debug("Log statistics request from user: {}", authentication.getName());

        Map<String, Object> stats = logService.getLogStatistics();
        return ResponseEntity.ok(stats);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteLog(@PathVariable String id,
                                       Authentication authentication) {
        log.warn("Log deletion request from user: {} for ID: {}",
                authentication.getName(), id);

        boolean deleted = logService.deleteLog(id);
        if (deleted) {
            return ResponseEntity.ok(Map.of("message", "Log deleted successfully"));
        }
        return ResponseEntity.notFound().build();
    }
}