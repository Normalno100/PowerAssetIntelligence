package com.powerassetintelligence.core.ai.rule;

import com.powerassetintelligence.core.ai.RiskFactor;
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
                    "AGE",
                    "Asset age is 25 years or more",
                    BigDecimal.valueOf(features.assetAgeYears()),
                    BigDecimal.valueOf(25),
                    "YEARS"
            );
            return Optional.of(RiskRuleResult.of(
                    "VERY_OLD_ASSET",
                    BigDecimal.valueOf(20),
                    "Asset age is 25 years or more",
                    List.of("Evaluate remaining useful life and replacement budget"),
                    factor
            ));
        }
        if (features.assetAgeYears() > 15) {
            RiskFactor factor = RiskFactor.of(
                    "AGING_ASSET",
                    "AGE",
                    "Asset age is above 15 years",
                    BigDecimal.valueOf(features.assetAgeYears()),
                    BigDecimal.valueOf(15),
                    "YEARS"
            );
            return Optional.of(RiskRuleResult.of(
                    "AGING_ASSET",
                    BigDecimal.valueOf(10),
                    "Asset age is above 15 years",
                    List.of("Increase diagnostic frequency"),
                    factor
            ));
        }
        return Optional.empty();
    }
}
