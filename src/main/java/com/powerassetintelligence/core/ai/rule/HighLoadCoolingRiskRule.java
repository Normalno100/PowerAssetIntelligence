package com.powerassetintelligence.core.ai.rule;

import com.powerassetintelligence.core.ai.RiskFactor;
import com.powerassetintelligence.core.ai.RiskFactorSeverity;
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

        boolean frequentOverheating = features.latestOverheatingCount() != null
                && features.latestOverheatingCount() >= 3;
        if (load.compareTo(BigDecimal.valueOf(90)) > 0 && frequentOverheating) {
            RiskFactor factor = RiskFactor.of(
                    "HIGH_LOAD_FREQUENT_OVERHEATING",
                    RiskFactorSeverity.HIGH,
                    "Load is above 90% and frequent overheating events were observed",
                    BigDecimal.valueOf(30)
            );
            return Optional.of(RiskRuleResult.of(
                    "HIGH_LOAD_FREQUENT_OVERHEATING",
                    factor,
                    List.of("Check cooling system", "Review operating mode and load redistribution")
            ));
        }
        if (load.compareTo(BigDecimal.valueOf(90)) > 0) {
            RiskFactor factor = RiskFactor.of(
                    "HIGH_LOAD",
                    RiskFactorSeverity.MEDIUM,
                    "Load is above 90%",
                    BigDecimal.valueOf(15)
            );
            return Optional.of(RiskRuleResult.of(
                    "HIGH_LOAD",
                    factor,
                    List.of("Monitor load trend and plan load balancing")
            ));
        }
        return Optional.empty();
    }
}
