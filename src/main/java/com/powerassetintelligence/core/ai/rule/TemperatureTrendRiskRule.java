package com.powerassetintelligence.core.ai.rule;

import com.powerassetintelligence.core.ai.RiskFactor;
import com.powerassetintelligence.core.ai.RiskFeatures;
import com.powerassetintelligence.core.ai.RiskRule;
import com.powerassetintelligence.core.ai.RiskRuleResult;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Risk rule that triggers when temperature is rising rapidly.
 * <p>
 * Unlike {@link HighTemperatureRiskRule} (which checks absolute temperature)
 * and {@link SustainedHighTemperatureRiskRule} (which checks sustained
 * high values), this rule detects a dangerous temperature <b>trend</b> —
 * the rate at which temperature is increasing per hour.
 *
 * <pre>
 * Example:
 *   latestTemperature = 85°C
 *   temperatureTrend   = +1.5°C/hour
 *   → HIGH risk (rapidly rising, already in danger zone)
 *
 *   latestTemperature = 85°C
 *   temperatureTrend   = +0.05°C/hour
 *   → no trigger (stable, already covered by SustainedHighTemperatureRiskRule)
 * </pre>
 */
public class TemperatureTrendRiskRule implements RiskRule {

    /**
     * Minimum positive trend (°C/hour) to consider it meaningful.
     * Prevents noise from triggering risk signals.
     */
    private static final BigDecimal TREND_NOISE_THRESHOLD = BigDecimal.valueOf(0.3);

    /**
     * Severity levels for rapid temperature rise.
     * Each level requires both a minimum trend AND a minimum latest temperature.
     */
    private static final Level[] SEVERITY_LEVELS = {
            new Level(
                    BigDecimal.valueOf(3.0),  // trend ≥ 3.0 °C/hour
                    BigDecimal.valueOf(70),    // latest ≥ 70 °C
                    BigDecimal.valueOf(30),    // score
                    "CRITICAL_TEMPERATURE_TREND",
                    "Temperature is rising critically fast: %s °C/hour (latest=%s °C)",
                    List.of("Invoke emergency maintenance protocol", "Reduce load immediately and dispatch inspection team")
            ),
            new Level(
                    BigDecimal.valueOf(2.0),  // trend ≥ 2.0 °C/hour
                    BigDecimal.valueOf(75),    // latest ≥ 75 °C
                    BigDecimal.valueOf(22),    // score
                    "HIGH_TEMPERATURE_TREND",
                    "Temperature is rising rapidly: %s °C/hour (latest=%s °C)",
                    List.of("Prioritize asset inspection within 2 hours", "Review cooling system performance")
            ),
            new Level(
                    BigDecimal.valueOf(1.0),  // trend ≥ 1.0 °C/hour
                    BigDecimal.valueOf(80),    // latest ≥ 80 °C
                    BigDecimal.valueOf(15),    // score
                    "ELEVATED_TEMPERATURE_TREND",
                    "Temperature trend is increasing: %s °C/hour (latest=%s °C)",
                    List.of("Increase monitoring frequency", "Prepare maintenance action plan")
            )
    };

    @Override
    public Optional<RiskRuleResult> evaluate(RiskFeatures features) {
        BigDecimal trend = features.temperatureTrendCelsiusPerHour();
        BigDecimal latestTemp = features.latestTemperatureCelsius();

        // No trend data available
        if (trend == null) {
            return Optional.empty();
        }

        // No latest temperature — cannot assess severity
        if (latestTemp == null) {
            return Optional.empty();
        }

        // Ignore negative or near-zero trends (covered by other rules)
        if (trend.compareTo(TREND_NOISE_THRESHOLD) <= 0) {
            return Optional.empty();
        }

        // Find the highest severity level that matches
        // (SEVERITY_LEVELS are ordered from highest to lowest, so break on first match)
        Level matchedLevel = null;
        for (Level level : SEVERITY_LEVELS) {
            if (trend.compareTo(level.minTrend) >= 0 && latestTemp.compareTo(level.minLatestTemp) >= 0) {
                matchedLevel = level;
                break;
            }
        }

        if (matchedLevel == null) {
            return Optional.empty();
        }

        String riskFactor = String.format(
                matchedLevel.description, trend, latestTemp);

        RiskFactor structuredFactor = RiskFactor.of(
                matchedLevel.code,
                "TEMPERATURE",
                "Temperature is rising rapidly",
                trend,
                matchedLevel.minTrend,
                "CELSIUS_PER_HOUR"
        );

        return Optional.of(RiskRuleResult.of(
                matchedLevel.code,
                matchedLevel.score,
                riskFactor,
                matchedLevel.recommendations,
                structuredFactor
        ));
    }

    /**
     * Internal class representing a severity level for temperature trend risk.
     */
    private record Level(
            BigDecimal minTrend,
            BigDecimal minLatestTemp,
            BigDecimal score,
            String code,
            String description,
            List<String> recommendations
    ) {
    }
}
