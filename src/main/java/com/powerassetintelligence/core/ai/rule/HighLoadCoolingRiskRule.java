package com.powerassetintelligence.core.ai.rule;

import com.powerassetintelligence.core.ai.RiskFeatures;
import com.powerassetintelligence.core.ai.RiskRule;
import com.powerassetintelligence.core.ai.RiskRuleResult;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public class HighLoadCoolingRiskRule implements RiskRule {

    @Override
    public Optional<RiskRuleResult> evaluate(RiskFeatures features) {
        BigDecimal load = features.latestLoadPercent();
        if (load == null) {
            return Optional.empty();
        }

        boolean frequentOverheating = features.latestOverheatingCount() != null && features.latestOverheatingCount() >= 3;
        if (load.compareTo(BigDecimal.valueOf(90)) > 0 && frequentOverheating) {
            return Optional.of(RiskRuleResult.of(
                    "HIGH_LOAD_FREQUENT_OVERHEATING",
                    BigDecimal.valueOf(30),
                    "Load is above 90% and frequent overheating events were observed",
                    List.of("Check cooling system", "Review operating mode and load redistribution")
            ));
        }
        if (load.compareTo(BigDecimal.valueOf(90)) > 0) {
            return Optional.of(RiskRuleResult.of(
                    "HIGH_LOAD",
                    BigDecimal.valueOf(15),
                    "Load is above 90%",
                    List.of("Monitor load trend and plan load balancing")
            ));
        }
        return Optional.empty();
    }
}
