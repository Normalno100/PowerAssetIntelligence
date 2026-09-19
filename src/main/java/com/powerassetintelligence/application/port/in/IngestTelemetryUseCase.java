package com.powerassetintelligence.application.port.in;

import com.powerassetintelligence.application.dto.TelemetryAcceptedResponse;
import com.powerassetintelligence.application.dto.TelemetryCreateCommand;

/**
 * Input port for ingesting telemetry data.
 */
public interface IngestTelemetryUseCase {
    TelemetryAcceptedResponse execute(TelemetryCreateCommand command);
}
