package com.powerassetintelligence.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Feature vector extracted from telemetry, maintenance, and asset data
 * for use by the risk scoring engine.
 */
public record RiskFeatures(
        UUID assetId,
        AssetType assetType,
        AssetStatus assetStatus,
        AssetCriticality criticality,
        int assetAgeYears,
        BigDecimal latestTemperatureCelsius,
        BigDecimal latestLoadPercent,
        Integer latestOverheatingCount,
        long repairsLastYear,
        // Statistical features from the last 24 hours
        BigDecimal averageTemperatureCelsius,
        BigDecimal maxTemperatureCelsius,
        BigDecimal averageLoadPercent,
        BigDecimal maxLoadPercent,
        long overheatingEventsLast24Hours,
        BigDecimal temperatureTrendCelsiusPerHour,
        BigDecimal loadTrendPercentPerHour
) {

    public boolean hasTelemetry() {
        return latestTemperatureCelsius != null || latestLoadPercent != null || latestOverheatingCount != null;
    }
}
