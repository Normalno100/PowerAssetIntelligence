package com.powerassetintelligence.application.dto;

import java.time.Instant;
import java.util.UUID;

public record TelemetryAcceptedResponse(
        UUID eventId,
        UUID assetId,
        String status,
        Instant acceptedAt
) {
}
