package com.powerassetintelligence.application.dto;

import com.powerassetintelligence.domain.model.AssetCriticality;
import com.powerassetintelligence.domain.model.AssetStatus;
import com.powerassetintelligence.domain.model.AssetType;
import java.math.BigDecimal;

/**
 * Response DTO representing the snapshot of input features used to compute a risk assessment.
 * <p>
 * Enables audit and reproducibility: the exact data used for scoring is preserved even
 * when the underlying telemetry or maintenance data changes.
 */
public record RiskAssessmentSnapshotResponse(
        // Asset metadata
        AssetType assetType,
        AssetStatus assetStatus,
        AssetCriticality criticality,
        int assetAgeYears,

        // Latest telemetry
        BigDecimal latestTemperatureCelsius,
        BigDecimal latestLoadPercent,
        Integer latestOverheatingCount,
        long repairsLastYear,

        // 24-hour statistics
        BigDecimal averageTemperatureCelsius,
        BigDecimal maxTemperatureCelsius,
        BigDecimal averageLoadPercent,
        BigDecimal maxLoadPercent,
        long overheatingEventsLast24Hours,

        // Trends
        BigDecimal temperatureTrendCelsiusPerHour,
        BigDecimal loadTrendPercentPerHour
) {
    /**
     * Creates a snapshot response from a domain snapshot.
     */
    public static RiskAssessmentSnapshotResponse from(com.powerassetintelligence.core.ai.RiskAssessmentSnapshot snapshot) {
        if (snapshot == null) {
            return null;
        }
        return new RiskAssessmentSnapshotResponse(
                snapshot.assetType(),
                snapshot.assetStatus(),
                snapshot.criticality(),
                snapshot.assetAgeYears(),
                snapshot.latestTemperatureCelsius(),
                snapshot.latestLoadPercent(),
                snapshot.latestOverheatingCount(),
                snapshot.repairsLastYear(),
                snapshot.averageTemperatureCelsius(),
                snapshot.maxTemperatureCelsius(),
                snapshot.averageLoadPercent(),
                snapshot.maxLoadPercent(),
                snapshot.overheatingEventsLast24Hours(),
                snapshot.temperatureTrendCelsiusPerHour(),
                snapshot.loadTrendPercentPerHour()
        );
    }
}
