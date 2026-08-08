package com.powerassetintelligence.infrastructure.web;

import com.powerassetintelligence.application.dto.TelemetryCreateCommand;
import com.powerassetintelligence.infrastructure.web.dto.TelemetryCreateRequest;

/**
 * Maps HTTP request DTOs to application commands for telemetry.
 *
 * This mapper converts {@link TelemetryCreateRequest} (infrastructure/web layer)
 * to {@link TelemetryCreateCommand} (application layer), performing any
 * necessary normalization of input values.
 */
public final class TelemetryWebMapper {

    private TelemetryWebMapper() {
    }

    /**
     * Converts an HTTP request to an application command.
     *
     * @param request the validated HTTP request DTO
     * @return application command ready for use by TelemetryService
     */
    public static TelemetryCreateCommand toCommand(TelemetryCreateRequest request) {
        return new TelemetryCreateCommand(
                request.assetId(),
                request.timestamp(),
                request.temperatureCelsius(),
                request.loadPercent(),
                request.voltageKv(),
                request.currentAmpere(),
                request.vibrationMmSec(),
                request.overheatingCount(),
                request.sourceSensorId(),
                request.externalTelemetryId()
        );
    }
}
