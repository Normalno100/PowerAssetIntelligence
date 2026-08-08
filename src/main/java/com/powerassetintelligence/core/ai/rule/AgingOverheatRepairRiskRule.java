package com.powerassetintelligence.core.ai.rule;

import com.powerassetintelligence.core.ai.RiskFeatures;
import com.powerassetintelligence.core.ai.RiskRule;
import com.powerassetintelligence.core.ai.RiskRuleResult;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public class AgingOverheatRepairRiskRule implements RiskRule {

    private static final BigDecimal TEMPERATURE_THRESHOLD = BigDecimal.valueOf(80);

    @Override
    public Optional<RiskRuleResult> evaluate(RiskFeatures features) {
        if (features.assetAgeYears() > 15
                && features.latestTemperatureCelsius() != null
                && features.latestTemperatureCelsius().compareTo(TEMPERATURE_THRESHOLD) > 0
                && features.repairsLastYear() > 3) {
            return Optional.of(RiskRuleResult.of(
                    "AGING_OVERHEAT_REPAIR_HISTORY",
                    BigDecimal.valueOf(45),
                    "Asset is older than 15 years, overheated above 80°C and had more than 3 repairs in the last year",
                    List.of("Schedule urgent engineering diagnostics", "Prepare repair or replacement plan")
            ));
        }
        return Optional.empty();
    }
}
