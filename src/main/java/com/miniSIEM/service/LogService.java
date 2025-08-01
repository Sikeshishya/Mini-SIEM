package com.miniSIEM.service;

import com.miniSIEM.model.LogEntry;
import com.miniSIEM.repository.LogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class LogService {
    private final LogRepository logRepository;

    public LogEntry saveLog(LogEntry logEntry) {
        logEntry.setTimestamp(Instant.now());
        return logRepository.save(logEntry);
    }
}