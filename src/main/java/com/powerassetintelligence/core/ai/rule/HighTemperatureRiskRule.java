package com.powerassetintelligence.core.ai.rule;

import com.powerassetintelligence.core.ai.RiskFactor;
import com.powerassetintelligence.core.ai.RiskFactorSeverity;
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
            RiskFactor factor = RiskFactor.of(
                    "CRITICAL_TEMPERATURE",
                    RiskFactorSeverity.CRITICAL,
                    "Latest temperature is at or above 95°C",
                    BigDecimal.valueOf(35)
            );
            return Optional.of(RiskRuleResult.of(
                    "CRITICAL_TEMPERATURE",
                    factor,
                    List.of("Dispatch inspection crew immediately", "Reduce load until diagnostics are completed")
            ));
        }
        if (temperature.compareTo(BigDecimal.valueOf(80)) > 0) {
            RiskFactor factor = RiskFactor.of(
                    "HIGH_TEMPERATURE",
                    RiskFactorSeverity.HIGH,
                    "Latest temperature is above 80°C",
                    BigDecimal.valueOf(20)
            );
            return Optional.of(RiskRuleResult.of(
                    "HIGH_TEMPERATURE",
                    factor,
                    List.of("Inspect thermal condition and cooling system")
            ));
        }
        return Optional.empty();
    }
}
