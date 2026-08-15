package com.powerassetintelligence.core.ai;

import com.powerassetintelligence.domain.model.AssetCriticality;
import com.powerassetintelligence.domain.model.AssetStatus;
import com.powerassetintelligence.domain.model.AssetType;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Immutable snapshot of risk features captured at the time of assessment.
 * <p>
 * Preserves the exact input data used to compute a {@link RiskAssessment},
 * enabling full reproducibility and auditability of risk decisions regardless
 * of how telemetry or maintenance data evolves over time.
 * <p>
 * All fields from {@link RiskFeatures} are included to ensure future-proofing:
 * new rules may reference fields that current rules do not use.
 *
 * @see RiskFeatures
 * @see RiskAssessment
 */
public record RiskAssessmentSnapshot(
        // --- Asset metadata ---
        AssetType assetType,
        AssetStatus assetStatus,
        AssetCriticality criticality,
        int assetAgeYears,

        // --- Latest telemetry ---
        BigDecimal latestTemperatureCelsius,
        BigDecimal latestLoadPercent,
        Integer latestOverheatingCount,
        long repairsLastYear,

        // --- 24-hour statistics ---
        BigDecimal averageTemperatureCelsius,
        BigDecimal maxTemperatureCelsius,
        BigDecimal averageLoadPercent,
        BigDecimal maxLoadPercent,
        long overheatingEventsLast24Hours,

        // --- Trends ---
        BigDecimal temperatureTrendCelsiusPerHour,
        BigDecimal loadTrendPercentPerHour
) {

    /**
     * Creates a snapshot from the given features.
     */
    public static RiskAssessmentSnapshot from(RiskFeatures features) {
        return new RiskAssessmentSnapshot(
                features.assetType(),
                features.assetStatus(),
                features.criticality(),
                features.assetAgeYears(),
                features.latestTemperatureCelsius(),
                features.latestLoadPercent(),
                features.latestOverheatingCount(),
                features.repairsLastYear(),
                features.averageTemperatureCelsius(),
                features.maxTemperatureCelsius(),
                features.averageLoadPercent(),
                features.maxLoadPercent(),
                features.overheatingEventsLast24Hours(),
                features.temperatureTrendCelsiusPerHour(),
                features.loadTrendPercentPerHour()
        );
    }
}
