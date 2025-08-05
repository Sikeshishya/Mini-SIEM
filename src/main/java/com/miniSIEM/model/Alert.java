package com.miniSIEM.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

@Document(collection = "alerts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Alert {
    @Id private String id;
    private String alertType;
    private String description;
    private String severity;
    private String sourceIp;
    private Instant timestamp;
    private boolean resolved;

    public static Alert createBruteForceAlert(String ip, int attemptCount) {
        return Alert.builder()
                .alertType("BRUTE_FORCE")
                .description(attemptCount + " failed logins from " + ip + " in 5 minutes")
                .severity("HIGH")
                .sourceIp(ip)
                .timestamp(Instant.now())
                .resolved(false)
                .build();
    }
}