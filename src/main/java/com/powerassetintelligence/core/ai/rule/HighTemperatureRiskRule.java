package com.powerassetintelligence.core.ai.rule;

import com.powerassetintelligence.core.ai.RiskFeatures;
import com.powerassetintelligence.core.ai.RiskRule;
import com.powerassetintelligence.core.ai.RiskRuleResult;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public class HighTemperatureRiskRule implements RiskRule {

    @Override
    public Optional<RiskRuleResult> evaluate(RiskFeatures features) {
        BigDecimal temperature = features.latestTemperatureCelsius();
        if (temperature == null) {
            return Optional.empty();
        }
        if (temperature.compareTo(BigDecimal.valueOf(95)) >= 0) {
            return Optional.of(RiskRuleResult.of(
                    "CRITICAL_TEMPERATURE",
                    BigDecimal.valueOf(35),
                    "Latest temperature is at or above 95°C",
                    List.of("Dispatch inspection crew immediately", "Reduce load until diagnostics are completed")
            ));
        }
        if (temperature.compareTo(BigDecimal.valueOf(80)) > 0) {
            return Optional.of(RiskRuleResult.of(
                    "HIGH_TEMPERATURE",
                    BigDecimal.valueOf(20),
                    "Latest temperature is above 80°C",
                    List.of("Inspect thermal condition and cooling system")
            ));
        }
        return Optional.empty();
    }
}
