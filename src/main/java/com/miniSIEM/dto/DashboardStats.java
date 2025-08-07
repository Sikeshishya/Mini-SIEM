package com.miniSIEM.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStats {
    private long totalLogs;
    private long logsLast24h;
    private long logsLastHour;
    private long activeThreats;
    private long criticalAlerts;
    private Map<String, Long> logsByLevel;
    private Map<String, Long> logsBySource;
    private double logsPerMinute;
    private String systemStatus;
    private Instant lastUpdated;
}