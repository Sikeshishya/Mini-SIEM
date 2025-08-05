package com.miniSIEM.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

@Document(collection = "logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogEntry {
    @Id
    private String id;
    private Instant timestamp;
    private String source;
    private String logLevel;
    private String message;
    private String ip;
}