package com.miniSIEM.controller;

import com.miniSIEM.model.LogEntry;
import com.miniSIEM.service.LogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class LogController {
    private final LogService logService; // Must be final for @RequiredArgsConstructor

    @PostMapping
    public ResponseEntity<LogEntry> ingestLog(@RequestBody LogEntry logEntry) {
        return ResponseEntity.ok(logService.saveLog(logEntry));
    }
}