package com.powerassetintelligence.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TelemetryCreateRequest(
        UUID assetId,
        Instant timestamp,
        BigDecimal temperatureCelsius,
        BigDecimal loadPercent,
        BigDecimal voltageKv,
        BigDecimal currentAmpere,
        BigDecimal vibrationMmSec,
        Integer overheatingCount,
        String sourceSensorId,
        String externalTelemetryId
) {
}
