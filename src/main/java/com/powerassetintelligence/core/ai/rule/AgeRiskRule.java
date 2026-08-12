package com.powerassetintelligence.core.ai.rule;

import com.powerassetintelligence.core.ai.RiskFactor;
import com.powerassetintelligence.core.ai.RiskFactorSeverity;
import com.powerassetintelligence.core.ai.RiskFeatures;
import com.powerassetintelligence.core.ai.RiskRule;
import com.powerassetintelligence.core.ai.RiskRuleResult;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public class AgeRiskRule implements RiskRule {

    @Override
    public Optional<RiskRuleResult> evaluate(RiskFeatures features) {
        if (features.assetAgeYears() >= 25) {
            RiskFactor factor = RiskFactor.of(
                    "VERY_OLD_ASSET",
                    RiskFactorSeverity.HIGH,
                    "Asset age is 25 years or more",
                    BigDecimal.valueOf(20)
            );
            return Optional.of(RiskRuleResult.of(
                    "VERY_OLD_ASSET",
                    factor,
                    List.of("Evaluate remaining useful life and replacement budget")
            ));
        }
        if (features.assetAgeYears() > 15) {
            RiskFactor factor = RiskFactor.of(
                    "AGING_ASSET",
                    RiskFactorSeverity.MEDIUM,
                    "Asset age is above 15 years",
                    BigDecimal.valueOf(10)
            );
            return Optional.of(RiskRuleResult.of(
                    "AGING_ASSET",
                    factor,
                    List.of("Increase diagnostic frequency")
            ));
        }
        return Optional.empty();
    }
}
