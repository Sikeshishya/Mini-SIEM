package com.miniSIEM.service;

import org.springframework.stereotype.Service;
import java.util.Set;

@Service
public class IpWhitelistService {
    private final Set<String> whitelistedIps = Set.of(
            "10.0.0.1",
            "192.168.1.1"
    );

    public boolean isAllowed(String ip) {
        return !whitelistedIps.contains(ip);
    }
}