package com.miniSIEM.controller;

import com.miniSIEM.model.Alert;
import com.miniSIEM.repository.AlertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertController {
    private final AlertRepository alertRepository;

    @GetMapping
    public List<Alert> getActiveAlerts() {
        return alertRepository.findByResolvedFalse();
    }

    @PostMapping("/{id}/resolve")
    public Alert resolveAlert(@PathVariable String id) {
        return alertRepository.findById(id)
                .map(alert -> {
                    alert.setResolved(true);
                    return alertRepository.save(alert);
                })
                .orElseThrow();
    }
}