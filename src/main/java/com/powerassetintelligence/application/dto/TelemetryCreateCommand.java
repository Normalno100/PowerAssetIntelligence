package com.powerassetintelligence.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Application-level command representing a telemetry ingestion request.
 *
 * This command is decoupled from infrastructure concerns (Spring MVC, Kafka).
 * It is created by TelemetryWebMapper from TelemetryCreateRequest and consumed
 * by TelemetryService for event publication and persistence operations.
 */
public record TelemetryCreateCommand(
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
    /**
     * Normalizes string fields by trimming whitespace and converting blanks to null.
     */
    public TelemetryCreateCommand {
        sourceSensorId = blankToNull(sourceSensorId != null ? sourceSensorId.trim() : null);
        externalTelemetryId = blankToNull(externalTelemetryId);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
