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
public class Alert {
    private String id;
    private String title;
    private String description;
    private String severity; // LOW, MEDIUM, HIGH, CRITICAL
    private String source;
    private String sourceIp;
    private Instant createdAt;
    private String status; // NEW, ACKNOWLEDGED, INVESTIGATING, RESOLVED
    private String assignedTo;
    private Map<String, Object> metadata;
}