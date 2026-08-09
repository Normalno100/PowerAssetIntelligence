package com.powerassetintelligence.core.ai.rule;

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
    private static final BigDecimal SUSTAINED_SCORE = BigDecimal.valueOf(12);
    private static final BigDecimal SUSTAINED_WITH_PEAK_SCORE = BigDecimal.valueOf(15);

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

        BigDecimal score = severePeak
                ? SUSTAINED_WITH_PEAK_SCORE
                : SUSTAINED_SCORE;

        String riskFactor;
        List<String> recommendations;

        if (severePeak) {
            riskFactor = String.format(
                    "Sustained high temperature: latest=%s°C, average24h=%s°C, max24h=%s°C (severe peak detected)",
                    latest, average, max);
            recommendations = List.of(
                    "Inspect asset for overheating damage and review thermal protection",
                    "Inspect cooling system and thermal conditions");
        } else {
            riskFactor = String.format(
                    "Sustained high temperature: latest=%s°C, average24h=%s°C, max24h=%s°C",
                    latest, average, max);
            recommendations = List.of(
                    "Inspect cooling system and thermal conditions");
        }

        return Optional.of(RiskRuleResult.of(
                "SUSTAINED_HIGH_TEMPERATURE",
                score,
                riskFactor,
                recommendations));
    }
}
