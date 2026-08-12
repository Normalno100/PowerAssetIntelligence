package com.powerassetintelligence.core.ai.rule;

import com.powerassetintelligence.core.ai.RiskFactor;
import com.powerassetintelligence.core.ai.RiskFactorSeverity;
import com.powerassetintelligence.core.ai.RiskFeatures;
import com.powerassetintelligence.core.ai.RiskRule;
import com.powerassetintelligence.core.ai.RiskRuleResult;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public class RepairHistoryRiskRule implements RiskRule {

    @Override
    public Optional<RiskRuleResult> evaluate(RiskFeatures features) {
        long repairs = features.repairsLastYear();
        if (repairs > 3) {
            RiskFactor factor = RiskFactor.of(
                    "FREQUENT_REPAIRS",
                    RiskFactorSeverity.HIGH,
                    "More than 3 maintenance records were registered during the last year",
                    BigDecimal.valueOf(20)
            );
            return Optional.of(RiskRuleResult.of(
                    "FREQUENT_REPAIRS",
                    factor,
                    List.of("Perform root-cause analysis of recurring failures")
            ));
        }
        if (repairs >= 2) {
            RiskFactor factor = RiskFactor.of(
                    "REPEATED_REPAIRS",
                    RiskFactorSeverity.MEDIUM,
                    "Repeated maintenance records were registered during the last year",
                    BigDecimal.valueOf(10)
            );
            return Optional.of(RiskRuleResult.of(
                    "REPEATED_REPAIRS",
                    factor,
                    List.of("Review maintenance effectiveness")
            ));
        }
        return Optional.empty();
    }
}
