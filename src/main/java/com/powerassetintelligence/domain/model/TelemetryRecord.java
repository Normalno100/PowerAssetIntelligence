package com.powerassetintelligence.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TelemetryRecord(UUID id, UUID assetId, Instant timestamp, BigDecimal temperatureCelsius,
        BigDecimal loadPercent, BigDecimal voltageKv, BigDecimal currentAmpere, BigDecimal vibrationMmSec,
        Integer overheatingCount, String sourceSensorId, String externalTelemetryId, Instant createdAt) {
    public TelemetryRecord(UUID id, UUID assetId, Instant timestamp, BigDecimal temperatureCelsius,
            BigDecimal loadPercent, BigDecimal voltageKv, BigDecimal currentAmpere, BigDecimal vibrationMmSec,
            Integer overheatingCount, String sourceSensorId, String externalTelemetryId) {
        this(id, assetId, timestamp, temperatureCelsius, loadPercent, voltageKv, currentAmpere, vibrationMmSec,
                overheatingCount, sourceSensorId, externalTelemetryId, null);
    }
}
