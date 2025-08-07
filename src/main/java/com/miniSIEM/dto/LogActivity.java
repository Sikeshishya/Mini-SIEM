package com.miniSIEM.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogActivity {
    private Instant timestamp;
    private long count;
    private String level;
    private String source;
}
