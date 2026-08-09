package com.powerassetintelligence.core.ai;

import com.powerassetintelligence.domain.model.AssetCriticality;
import com.powerassetintelligence.domain.model.AssetStatus;
import com.powerassetintelligence.domain.model.AssetType;
import java.math.BigDecimal;
import java.util.UUID;

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
        long overheatingEventsLast24Hours
) {

    public boolean hasTelemetry() {
        return latestTemperatureCelsius != null || latestLoadPercent != null || latestOverheatingCount != null;
    }
}
