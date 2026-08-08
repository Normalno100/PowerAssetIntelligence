package com.powerassetintelligence.core.ai.rule;

import com.powerassetintelligence.core.ai.RiskFeatures;
import com.powerassetintelligence.core.ai.RiskRule;
import com.powerassetintelligence.core.ai.RiskRuleResult;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public class RepairHistoryRiskRule implements RiskRule {

    @Override
    public Optional<RiskRuleResult> evaluate(RiskFeatures features) {
        if (features.repairsLastYear() > 3) {
            return Optional.of(RiskRuleResult.of(
                    "FREQUENT_REPAIRS",
                    BigDecimal.valueOf(20),
                    "More than 3 maintenance records were registered during the last year",
                    List.of("Perform root-cause analysis of recurring failures")
            ));
        }
        if (features.repairsLastYear() >= 2) {
            return Optional.of(RiskRuleResult.of(
                    "REPEATED_REPAIRS",
                    BigDecimal.valueOf(10),
                    "Repeated maintenance records were registered during the last year",
                    List.of("Review maintenance effectiveness")
            ));
        }
        return Optional.empty();
    }
}
