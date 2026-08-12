package com.powerassetintelligence.core.ai.rule;

import com.powerassetintelligence.core.ai.RiskFactor;
import com.powerassetintelligence.core.ai.RiskFactorSeverity;
import com.powerassetintelligence.core.ai.RiskFeatures;
import com.powerassetintelligence.core.ai.RiskRule;
import com.powerassetintelligence.core.ai.RiskRuleResult;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public class SustainedHighTemperatureRiskRule implements RiskRule {

    private static final BigDecimal SUSTAINED_THRESHOLD_LATEST = BigDecimal.valueOf(85);
    private static final BigDecimal SUSTAINED_THRESHOLD_AVERAGE = BigDecimal.valueOf(75);
    private static final BigDecimal SEVERE_PEAK_THRESHOLD = BigDecimal.valueOf(90);

    @Override
    public Optional<RiskRuleResult> evaluate(RiskFeatures features) {
        BigDecimal latest = features.latestTemperatureCelsius();
        BigDecimal average = features.averageTemperatureCelsius();
        BigDecimal max = features.maxTemperatureCelsius();

        boolean sustainedHigh = latest != null
                && average != null
                && latest.compareTo(SUSTAINED_THRESHOLD_LATEST) >= 0
                && average.compareTo(SUSTAINED_THRESHOLD_AVERAGE) >= 0;

        boolean severePeak = max != null && max.compareTo(SEVERE_PEAK_THRESHOLD) >= 0;

        if (!sustainedHigh) {
            return Optional.empty();
        }

        RiskFactorSeverity severity = severePeak
                ? RiskFactorSeverity.HIGH
                : RiskFactorSeverity.MEDIUM;
        BigDecimal contribution = severePeak
                ? BigDecimal.valueOf(15)
                : BigDecimal.valueOf(12);

        String description = severePeak
                ? "Severe sustained high temperature peak detected"
                : "Sustained high temperature over 24h";

        List<String> recommendations = severePeak
                ? List.of(
                        "Inspect asset for overheating damage and review thermal protection",
                        "Inspect cooling system and thermal conditions")
                : List.of("Inspect cooling system and thermal conditions");

        RiskFactor factor = RiskFactor.of(
                "SUSTAINED_HIGH_TEMPERATURE",
                severity,
                description,
                contribution);

        return Optional.of(RiskRuleResult.of(
                "SUSTAINED_HIGH_TEMPERATURE",
                factor,
                recommendations));
    }
}
