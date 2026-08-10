package com.powerassetintelligence.core.ai.rule;

import com.powerassetintelligence.core.ai.RiskFactor;
import com.powerassetintelligence.core.ai.RiskFeatures;
import com.powerassetintelligence.core.ai.RiskRule;
import com.powerassetintelligence.core.ai.RiskRuleResult;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public class RepairHistoryRiskRule implements RiskRule {

    @Override
    public Optional<RiskRuleResult> evaluate(RiskFeatures features) {
        BigDecimal repairs = BigDecimal.valueOf(features.repairsLastYear());
        if (repairs.intValue() > 3) {
            RiskFactor factor = RiskFactor.of(
                    "FREQUENT_REPAIRS",
                    "MAINTENANCE",
                    "More than 3 maintenance records were registered during the last year",
                    repairs,
                    BigDecimal.valueOf(3),
                    "COUNT"
            );
            return Optional.of(RiskRuleResult.of(
                    "FREQUENT_REPAIRS",
                    BigDecimal.valueOf(20),
                    "More than 3 maintenance records were registered during the last year",
                    List.of("Perform root-cause analysis of recurring failures"),
                    factor
            ));
        }
        if (repairs.intValue() >= 2) {
            RiskFactor factor = RiskFactor.of(
                    "REPEATED_REPAIRS",
                    "MAINTENANCE",
                    "Repeated maintenance records were registered during the last year",
                    repairs,
                    BigDecimal.valueOf(2),
                    "COUNT"
            );
            return Optional.of(RiskRuleResult.of(
                    "REPEATED_REPAIRS",
                    BigDecimal.valueOf(10),
                    "Repeated maintenance records were registered during the last year",
                    List.of("Review maintenance effectiveness"),
                    factor
            ));
        }
        return Optional.empty();
    }
}
