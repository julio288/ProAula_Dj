package com.taller.registro_login.dto;

import java.time.LocalDateTime;

public record MonitoringSummary(
        int configuredServices,
        int availableServices,
        int unavailableServices,
        LocalDateTime lastCheckedAt,
        String statusLabel) {
}
